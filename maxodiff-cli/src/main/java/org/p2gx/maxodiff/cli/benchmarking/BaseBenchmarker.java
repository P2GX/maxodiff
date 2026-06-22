package org.p2gx.maxodiff.cli.benchmarking;

import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.p2gx.maxodiff.core.analysis.HpoFrequency;
import org.p2gx.maxodiff.core.analysis.MySimpleTerm;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;

import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.model.*;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class BaseBenchmarker {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseBenchmarker.class);

    private final PhenopacketData sample;
    private final int nDiseases;
    private final int nRepetitions;
    private final DDxEngine phenomizer;
    private final HpoDiseases hpoDiseases;
    private final Map<MySimpleTerm, Set<MySimpleTerm>> hpoTermToMaxoTermSetMap;
    private final DiffDiagRefiner refiner;
    private final MinimalOntology ontology;
 //   private final Map<MySimpleTerm, Set<MySimpleTerm>> hpoToMaxoTermMap;
    private final List<HpoFrequency> hpoFrequencies;

    public List<DifferentialDiagnosis> getCompleteInitialDiffDiagList() {
        return completeInitialDiffDiagList;
    }

    private final List<DifferentialDiagnosis> completeInitialDiffDiagList;
    private final MaxoHpoTermProbabilities maxoHpoTermProbabilities;

    public PhenopacketData getSample() {
        return sample;
    }


    public BaseBenchmarker(PhenopacketData sample,
                           MdContext context,
                           DDxEngine phenomizer,

                           List<HpoFrequency> hpoFrequencies) {

        this.nDiseases = context.params().nDiseases();
        this.nRepetitions = context.params().nRepetitions();
        this.phenomizer = phenomizer;
        this.hpoDiseases = context.resources().hpoDiseases();
        this.hpoTermToMaxoTermSetMap = context.resources().maxoAnnotsMap();
        this.refiner = context.createRefiner();
        this.ontology = context.resources().hpo();
        this.sample = sample;
        this.hpoFrequencies = hpoFrequencies;
        this.completeInitialDiffDiagList = determineInitialDiagnoses();
        this.maxoHpoTermProbabilities = calculateMaxoHpoTermProbabilities();
    }


    public List<DifferentialDiagnosis> getTopNInitialDiffDiagList() {
        return completeInitialDiffDiagList.subList(0, nDiseases);
    }

    public MaxoHpoTermProbabilities getMaxoHpoTermProbabilities() {
        return maxoHpoTermProbabilities;
    }



    /*** @return List with top initial (i.e., before Maxodiff) {@code nDiseases} differential diagnoses using Phenomizer */
    private List<DifferentialDiagnosis> determineInitialDiagnoses() {
        LOGGER.info(String.valueOf(sample.sampleId()));
        LOGGER.info("nDiseases = {}", nDiseases);
        List<DifferentialDiagnosis> differentialDiagnoses = phenomizer.run(sample);
//        differentialDiagnoses.sort(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed());
        return differentialDiagnoses;
    }

    private MaxoHpoTermProbabilities calculateMaxoHpoTermProbabilities() {
        return new MaxoHpoTermProbabilities(hpoDiseases,
                hpoTermToMaxoTermSetMap,
                getTopNInitialDiffDiagList());
    }

    public List<RankedMaxoResult> standardRun(BiometadataService biometadataService) throws Exception {
        LOGGER.info("sample = {}, n Diseases = {}, n Repetitions = {}", this.sample.sampleId(), nDiseases, nRepetitions);
        List<DifferentialDiagnosis> topNinitialDiffDiagList = getTopNInitialDiffDiagList();
        Set<TermId> topNInitialDiagnosesIds = topNinitialDiffDiagList.stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<HpoDisease> diseases = refiner.getDiseases(topNinitialDiffDiagList);
        List<HpoFrequency> hpoTermCounts = refiner.getHpoFrequenciesNDiseases(diseases, hpoFrequencies);
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = refiner.getMaxoToHpoTermIdMap(hpoTermCounts);

        RankMaxo rankMaxo = new RankMaxo(maxoToHpoTermIdMap,
                maxoHpoTermProbabilities, phenomizer,
                ontology, getCompleteInitialDiffDiagList(), topNinitialDiffDiagList, hpoTermCounts);

        return refiner.run(sample,
                topNInitialDiagnosesIds,
                rankMaxo);
    }

    /**
     * Take the diagnosis that was at rank diseaseIndex after the initial phenomizer run, shuffle the entire
     * List of HPO Diseases and choose nDiseases of these (e.g. nDiseases=20), and then spike the above disease
     * at rank nDiseases
     * @param diseaseIndex
     * @return
     * @throws Exception
     */
    public List<RankedMaxoResult> spikedRandomizer(int diseaseIndex, BiometadataService biometadataService) throws Exception {
        List<DifferentialDiagnosis> diseaseTopNList = getTopNInitialDiffDiagList();
        List<DifferentialDiagnosis> shuffledDiagnoses = new ArrayList<>(getCompleteInitialDiffDiagList());
        Collections.shuffle(shuffledDiagnoses);
        List<DifferentialDiagnosis> initialDiagnosesNDiseasesRandom = shuffledDiagnoses.subList(0, nDiseases);
        DifferentialDiagnosis originalDiagnosis = diseaseTopNList.get(diseaseIndex);
        LOGGER.info("spike disease {} (index {})", originalDiagnosis.diseaseId().toString(), (diseaseIndex + 1));
        initialDiagnosesNDiseasesRandom.set(initialDiagnosesNDiseasesRandom.size() - 1, originalDiagnosis);
        Set<TermId> topNInitialDiagnosesIdsRandom = initialDiagnosesNDiseasesRandom.stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<HpoDisease> diseases = refiner.getDiseases(initialDiagnosesNDiseasesRandom);
        List<HpoFrequency> hpoTermCounts = refiner.getHpoFrequenciesNDiseases(diseases, hpoFrequencies);
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = refiner.getMaxoToHpoTermIdMap(hpoTermCounts);

        RankMaxo rankMaxo = new RankMaxo(maxoToHpoTermIdMap,
                maxoHpoTermProbabilities, phenomizer,
                ontology, getCompleteInitialDiffDiagList(), initialDiagnosesNDiseasesRandom, hpoTermCounts);

        return refiner.run(sample,
                topNInitialDiagnosesIdsRandom,
                rankMaxo);
    }

    /**
     * Shuffle the entire list of HPO Diseases and choose nDiseases of these (e.g. nDiseases=20),
     * and then run refiner again
     * @return
     * @throws Exception
     */
    public List<RankedMaxoResult> shuffledRandomizer(BiometadataService biometadataService) throws Exception {
        List<DifferentialDiagnosis> shuffledDiagnoses = new ArrayList<>(getCompleteInitialDiffDiagList());
        Collections.shuffle(shuffledDiagnoses);
        List<DifferentialDiagnosis> initialDiagnosesNDiseasesRandom = shuffledDiagnoses.subList(0, nDiseases);
        Set<TermId> topNInitialDiagnosesIdsRandom = initialDiagnosesNDiseasesRandom.stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<HpoDisease> diseases = refiner.getDiseases(initialDiagnosesNDiseasesRandom);
        List<HpoFrequency> hpoTermCounts = refiner.getHpoFrequenciesNDiseases(diseases, hpoFrequencies);
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = refiner.getMaxoToHpoTermIdMap(hpoTermCounts);

        RankMaxo rankMaxo = new RankMaxo(maxoToHpoTermIdMap,
                maxoHpoTermProbabilities, phenomizer,
                ontology, getCompleteInitialDiffDiagList(), initialDiagnosesNDiseasesRandom, hpoTermCounts);

        return refiner.run(sample,
                topNInitialDiagnosesIdsRandom,
                rankMaxo);
    }



}
