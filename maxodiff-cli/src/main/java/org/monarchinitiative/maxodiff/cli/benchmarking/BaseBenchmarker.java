package org.monarchinitiative.maxodiff.cli.benchmarking;

import org.monarchinitiative.maxodiff.config.MaxodiffPropsConfiguration;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementOptions;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementResults;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.*;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

public class BaseBenchmarker {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseBenchmarker.class);

    private final Path phenopacketPath;
    private final Sample sample;
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

    public Sample getSample() {
        return sample;
    }

    public BaseBenchmarker(Path phenopacketPath,
                           RefinementOptions refinementOptions,
                           DifferentialDiagnosisEngine phenomizer,
                           HpoDiseases hpoDiseases,
                           MaxodiffPropsConfiguration maxoDiffConfig,
                           DiffDiagRefiner refiner ) {

        this.phenopacketPath = phenopacketPath;
        this.nDiseases = refinementOptions.nDiseases();
        this.nRepetitions = refinementOptions.nRepetitions();
        this.phenomizer = phenomizer;
        this.hpoDiseases = hpoDiseases;
        this.hpoTermToMaxoTermSetMap = maxoDiffConfig.maxoAnnotsMap();
        this.refiner = refiner;
        this.ontology = maxoDiffConfig.hpo();
        this.hpoToMaxoTermMap = maxoDiffConfig.maxoAnnotsMap();
        PhenopacketData phenopacketData = PhenopacketData.readPhenopacketData(this.phenopacketPath);
        this.sample = Sample.of(phenopacketData.sampleId(),
                phenopacketData.observedHpoTermIds().toList(),
                phenopacketData.excludedHpoTermIds().toList());
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
        LOGGER.info(String.valueOf(phenopacketPath));
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


    public List<MaxodiffResult> standardRun() throws Exception {
        RefinementOptions options = RefinementOptions.of(nDiseases, nRepetitions);
        LOGGER.info("ppkt = {}, n Diseases = {}, n Repetitions = {}", this.phenopacketPath.toFile().getName(), nDiseases, nRepetitions);
        List<DifferentialDiagnosis> topNinitialDiffDiagList = getTopNInitialDiffDiagList();
        List<HpoDisease> diseases = refiner.getDiseases(topNinitialDiffDiagList);
        Map<TermId, List<HpoFrequency>> hpoTermCounts = refiner.getHpoTermCounts(diseases);
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = refiner.getMaxoToHpoTermIdMap(hpoTermCounts);

        RankMaxo rankMaxo = new RankMaxo(hpoToMaxoTermMap, maxoToHpoTermIdMap,
                maxoHpoTermProbabilities, phenomizer,
                ontology, getCompleteInitialDiffDiagList(), topNinitialDiffDiagList);

        RefinementResults refinementResults = refiner.run(sample,
                completeInitialDiffDiagList,
                options,
                rankMaxo,
                hpoTermCounts,
                maxoToHpoTermIdMap);

        // Sort refinement results and write to files
        List<MaxodiffResult> resultsList = new ArrayList<>(refinementResults.maxodiffResults().stream().toList());
        resultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.rankMaxoScore().maxoScore()).reversed());
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
    public List<MaxodiffResult> spikedRandomizer(int diseaseIndex) throws Exception {
        List<DifferentialDiagnosis> diseaseTopNList = getTopNInitialDiffDiagList();
        List<DifferentialDiagnosis> shuffledDiagnoses = new ArrayList<>(getCompleteInitialDiffDiagList());
        Collections.shuffle(shuffledDiagnoses);
        List<DifferentialDiagnosis> initialDiagnosesNDiseasesRandom = shuffledDiagnoses.subList(0, nDiseases);
        DifferentialDiagnosis originalDiagnosis = diseaseTopNList.get(diseaseIndex);
        LOGGER.info("spike disease {} (index {})", originalDiagnosis.diseaseId().toString(), (diseaseIndex + 1));
        initialDiagnosesNDiseasesRandom.set(initialDiagnosesNDiseasesRandom.size() - 1, originalDiagnosis);

        List<HpoDisease> diseases = refiner.getDiseases(initialDiagnosesNDiseasesRandom);
        Map<TermId, List<HpoFrequency>> hpoTermCounts = refiner.getHpoTermCounts(diseases);
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = refiner.getMaxoToHpoTermIdMap(hpoTermCounts);

        RankMaxo rankMaxo = new RankMaxo(hpoToMaxoTermMap, maxoToHpoTermIdMap,
                maxoHpoTermProbabilities, phenomizer,
                ontology, getCompleteInitialDiffDiagList(), initialDiagnosesNDiseasesRandom);
        RefinementResults refinementResults = refiner.run(sample,
                initialDiagnosesNDiseasesRandom,
                new RefinementOptions(nDiseases, nRepetitions),
                rankMaxo,
                hpoTermCounts,
                maxoToHpoTermIdMap);

        return refinementResults.maxodiffResults().stream()
                .sorted(Comparator.comparingDouble((MaxodiffResult mr) -> mr.rankMaxoScore().maxoScore()).reversed())
                .toList();
    }

    /**
     * Shuffle the entire list of HPO Diseases and choose nDiseases of these (e.g. nDiseases=20),
     * and then run refiner again
     * @return
     * @throws Exception
     */
    public List<MaxodiffResult> shuffledRandomizer() throws Exception {
        List<DifferentialDiagnosis> shuffledDiagnoses = new ArrayList<>(getCompleteInitialDiffDiagList());
        Collections.shuffle(shuffledDiagnoses);
        List<DifferentialDiagnosis> initialDiagnosesNDiseasesRandom = shuffledDiagnoses.subList(0, nDiseases);

        List<HpoDisease> diseases = refiner.getDiseases(initialDiagnosesNDiseasesRandom);
        Map<TermId, List<HpoFrequency>> hpoTermCounts = refiner.getHpoTermCounts(diseases);
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = refiner.getMaxoToHpoTermIdMap(hpoTermCounts);

        RankMaxo rankMaxo = new RankMaxo(hpoToMaxoTermMap, maxoToHpoTermIdMap,
                maxoHpoTermProbabilities, phenomizer,
                ontology, getCompleteInitialDiffDiagList(), initialDiagnosesNDiseasesRandom);
        RefinementResults refinementResults = refiner.run(sample,
                initialDiagnosesNDiseasesRandom,
                new RefinementOptions(nDiseases, nRepetitions),
                rankMaxo,
                hpoTermCounts,
                maxoToHpoTermIdMap);

        return refinementResults.maxodiffResults().stream()
                .sorted(Comparator.comparingDouble((MaxodiffResult mr) -> mr.rankMaxoScore().maxoScore()).reversed())
                .toList();
    }



}
