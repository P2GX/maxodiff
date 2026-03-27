package org.p2gx.maxodiff.cli.benchmarking;

import org.p2gx.maxodiff.config.MaxodiffPropsConfiguration;
import org.p2gx.maxodiff.core.analysis.HpoFrequency;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.p2gx.maxodiff.core.analysis.refinement.RefinementOptions;
import org.p2gx.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.p2gx.maxodiff.core.model.*;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class BaseBenchmarker {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseBenchmarker.class);

    private final PpktSample sample;
    private final int nDiseases;
    private final int nRepetitions;
    private final DifferentialDiagnosisEngine phenomizer;
    private final HpoDiseases hpoDiseases;
    private final Map<SimpleTerm, Set<SimpleTerm>> hpoTermToMaxoTermSetMap;
    private final DiffDiagRefiner refiner;
    private final Ontology ontology;
    private final Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap;

    public List<DifferentialDiagnosis> getCompleteInitialDiffDiagList() {
        return completeInitialDiffDiagList;
    }

    private final List<DifferentialDiagnosis> completeInitialDiffDiagList;
    private final MaxoHpoTermProbabilities maxoHpoTermProbabilities;

    public PpktSample getSample() {
        return sample;
    }

    public BaseBenchmarker(PpktSample sample,
                           RefinementOptions refinementOptions,
                           DifferentialDiagnosisEngine phenomizer,
                           HpoDiseases hpoDiseases,
                           MaxodiffPropsConfiguration maxoDiffConfig,
                           DiffDiagRefiner refiner ) {

        this.nDiseases = refinementOptions.nDiseases();
        this.nRepetitions = refinementOptions.nRepetitions();
        this.phenomizer = phenomizer;
        this.hpoDiseases = hpoDiseases;
        this.hpoTermToMaxoTermSetMap = maxoDiffConfig.maxoAnnotsMap();
        this.refiner = refiner;
        this.ontology = maxoDiffConfig.hpo();
        this.hpoToMaxoTermMap = maxoDiffConfig.maxoAnnotsMap();
        this.sample = sample;
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
        LOGGER.info(String.valueOf(sample.id()));
        LOGGER.info("nDiseases = {}", nDiseases);
        List<DifferentialDiagnosis> differentialDiagnoses = phenomizer.run(sample);
//        differentialDiagnoses.sort(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed());
        return differentialDiagnoses;
    }

    private MaxoHpoTermProbabilities calculateMaxoHpoTermProbabilities() {
        DiseaseModelProbability diseaseModelProbability = DiseaseModelProbability.ranked(getTopNInitialDiffDiagList());
        return new MaxoHpoTermProbabilities(hpoDiseases,
                hpoTermToMaxoTermSetMap,
                getTopNInitialDiffDiagList(),
                diseaseModelProbability);
    }

    public List<RankedMaxoResult> standardRun(BiometadataService biometadataService) throws Exception {
        RefinementOptions options = RefinementOptions.of(nDiseases, nRepetitions);
        LOGGER.info("sample = {}, n Diseases = {}, n Repetitions = {}", this.sample.id(), nDiseases, nRepetitions);
        List<DifferentialDiagnosis> topNinitialDiffDiagList = getTopNInitialDiffDiagList();
        Set<TermId> topNInitialDiagnosesIds = topNinitialDiffDiagList.stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<HpoDisease> diseases = refiner.getDiseases(topNinitialDiffDiagList);
        Map<String, List<HpoFrequency>> hpoTermCounts = refiner.getHpoTermCounts(diseases);
        Map<String, Set<String>> maxoToHpoTermIdMap = refiner.getMaxoToHpoTermIdMap(hpoTermCounts);

        RankMaxo rankMaxo = new RankMaxo(hpoToMaxoTermMap, maxoToHpoTermIdMap,
                maxoHpoTermProbabilities, phenomizer,
                ontology, getCompleteInitialDiffDiagList(), topNinitialDiffDiagList);

        List<RankedMaxoResult> resultsList = refiner.runNew(sample,
                topNInitialDiagnosesIds,
                options,
                rankMaxo,
                biometadataService);

        return resultsList;
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
        Map<String, List<HpoFrequency>> hpoTermCounts = refiner.getHpoTermCounts(diseases);
        Map<String, Set<String>> maxoToHpoTermIdMap = refiner.getMaxoToHpoTermIdMap(hpoTermCounts);

        RankMaxo rankMaxo = new RankMaxo(hpoToMaxoTermMap, maxoToHpoTermIdMap,
                maxoHpoTermProbabilities, phenomizer,
                ontology, getCompleteInitialDiffDiagList(), initialDiagnosesNDiseasesRandom);
        List<RankedMaxoResult> refinementResults = refiner.runNew(sample,
                topNInitialDiagnosesIdsRandom,
                new RefinementOptions(nDiseases, nRepetitions),
                rankMaxo,
                biometadataService);

        return refinementResults;
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
        Map<String, List<HpoFrequency>> hpoTermCounts = refiner.getHpoTermCounts(diseases);
        Map<String, Set<String>> maxoToHpoTermIdMap = refiner.getMaxoToHpoTermIdMap(hpoTermCounts);

        RankMaxo rankMaxo = new RankMaxo(hpoToMaxoTermMap, maxoToHpoTermIdMap,
                maxoHpoTermProbabilities, phenomizer,
                ontology, getCompleteInitialDiffDiagList(), initialDiagnosesNDiseasesRandom);
        List<RankedMaxoResult> refinementResults = refiner.runNew(sample,
                topNInitialDiagnosesIdsRandom,
                new RefinementOptions(nDiseases, nRepetitions),
                rankMaxo,
                biometadataService);

        return refinementResults;
    }



}
