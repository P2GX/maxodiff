package org.monarchinitiative.maxodiff.core.analysis.refinement;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.*;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;

public class BaseDiffDiagRefiner implements DiffDiagRefiner {

    private final HpoDiseases hpoDiseases;
    private final Map<TermId, Set<TermId>> fullHpoToMaxoTermIdMap;
    private final Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap;
    private final MinimalOntology hpo;

    public BaseDiffDiagRefiner(HpoDiseases hpoDiseases,
                               Map<TermId, Set<TermId>> fullHpoToMaxoTermIdMap,
                               Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap,
                               MinimalOntology hpo) {
        this.hpoDiseases = hpoDiseases;
        this.fullHpoToMaxoTermIdMap = fullHpoToMaxoTermIdMap;
        this.hpoToMaxoTermMap = hpoToMaxoTermMap;
        this.hpo = hpo;
    }


    @Override
    public RefinementResults run(Sample sample,
                                 Collection<DifferentialDiagnosis> differentialDiagnoses,
                                 RefinementOptions options,
                                 DifferentialDiagnosisEngine engine,
                                 Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                                 Map<TermId, List<HpoFrequency>> hpoTermCounts,
                                 Map<TermId, List<DifferentialDiagnosis>> maxoTermToDDEngineDiagnosesMap) {
        // Get list of Hpo diseases
        List<HpoDisease> diseases = getDiseases(differentialDiagnoses.stream().toList());
        // Calculate final scores and make list of MaxodiffResult objects.
        List<MaxodiffResult> maxodiffResultsList = new ArrayList<>();
        for (TermId maxoId : maxoToHpoTermIdMap.keySet()) {
            // Get the set of HPO terms that can be ascertained by the MAXO term
            Set<TermId> hpoTermIds = maxoToHpoTermIdMap.get(maxoId);
            // Get HPO term combinations
            List<List<TermId>> hpoCombos = AnalysisUtils.getHpoTermCombos(maxoToHpoTermIdMap, maxoId);
            // Calculate final score and make score record
            MaxoTermScore maxoTermScore = AnalysisUtils.getMaxoTermScoreRecord(hpoTermIds,
                    hpoCombos,
                    maxoId,
                    differentialDiagnoses.stream().toList(),
                    diseases,
                    options);
            // Get HPO frequency records
            List<Frequencies> frequencies = getFrequencyRecords(maxoTermScore.omimTermIds(),
                    maxoTermScore.hpoTermIds(), hpoTermCounts);
            // Make dummy RankMaxoScore record
            RankMaxoScore rankMaxoScore = new RankMaxoScore(maxoId, maxoTermScore.omimTermIds(), maxoTermScore.maxoOmimTermIds(),
                    Set.of(), maxoTermScore.scoreDiff(), List.of(), Map.of(), Map.of(), 0, 0);
            // Make MaxodiffResult for the MAXO term
            MaxodiffResult maxodiffResult = new MaxodiffResultImpl(maxoTermScore, rankMaxoScore, frequencies, List.of());
            maxodiffResultsList.add(maxodiffResult);
        }
        maxodiffResultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.maxoTermScore().scoreDiff()).reversed());
        return new RefinementResultsImpl(maxodiffResultsList);
    }

    @Override
    public RefinementResults run(Sample sample,
                                 Collection<DifferentialDiagnosis> differentialDiagnoses,
                                 RefinementOptions options,
                                 RankMaxo rankMaxo,
                                 Map<TermId, List<HpoFrequency>> hpoTermCounts,
                                 Map<TermId, Set<TermId>> maxoToHpoTermIdMap) {

        List<MaxodiffResult> maxodiffResultsList = new ArrayList<>();
        List<DifferentialDiagnosis> initialDiagnoses = differentialDiagnoses.stream()
                .toList().subList(0, options.nDiseases());

        Set<TermId> initialDiagnosesIds = initialDiagnoses.stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toSet());

        Map<TermId, RankMaxoScore> maxoTermRanks = rankMaxo.rankMaxoTerms(sample, options.nRepetitions(), initialDiagnosesIds);
        for (Map.Entry<TermId, RankMaxoScore> entry : maxoTermRanks.entrySet()) {
            TermId maxoId = entry.getKey();
            RankMaxoScore rankMaxoScore = entry.getValue();
            double scoreDiff = rankMaxoScore.maxoScore();
            Set<TermId> diseaseIds = new LinkedHashSet<>();
            List<DifferentialDiagnosis> differentialDiagnosisModels = new ArrayList<>(differentialDiagnoses);
            differentialDiagnosisModels.sort(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed());
            differentialDiagnosisModels.forEach(d -> diseaseIds.add(d.diseaseId()));
            Set<TermId> hpoTermIds = maxoToHpoTermIdMap.get(maxoId);
            int nHpoTerms = hpoTermIds.size();

            MaxoTermScore maxoTermScore = new MaxoTermScore(maxoId.toString(), options.nDiseases(),
                    diseaseIds, Set.of(), nHpoTerms, hpoTermIds,
                    0.0, 0.0, scoreDiff, TermId.of("HP:000000"),
                    List.of(), List.of(),null,null);
            // Get HPO frequency records
            List<Frequencies> frequencies = getFrequencyRecords(maxoTermScore.omimTermIds(),
                    maxoTermScore.hpoTermIds(), hpoTermCounts);
            // Make MaxodiffResult for the MAXO term
            MaxodiffResult maxodiffResult = new MaxodiffResultImpl(maxoTermScore, rankMaxoScore, frequencies, List.of());
            maxodiffResultsList.add(maxodiffResult);
        }
        // Return RefinementResults object, which contains the list of MaxodiffResult objects.
        return new RefinementResultsImpl(maxodiffResultsList);
    }

    public RankMaxo getRankMaxo(List<DifferentialDiagnosis> initialDiagnoses,
                                 DifferentialDiagnosisEngine engine,
                                 Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                                 String diseaseProbModel) {

        DiseaseModelProbability diseaseModelProbability = DiseaseModelProbability.ranked(initialDiagnoses);
        switch (diseaseProbModel) {
            case "ranked" -> diseaseModelProbability = DiseaseModelProbability.ranked(initialDiagnoses);
            case "softmax" -> diseaseModelProbability = DiseaseModelProbability.softmax(initialDiagnoses);
            case "expDecay" -> diseaseModelProbability = DiseaseModelProbability.exponentialDecay(initialDiagnoses);
        }

        MaxoHpoTermProbabilities maxoHpoTermProbabilities = new MaxoHpoTermProbabilities(hpoDiseases,
                hpoToMaxoTermMap,
                initialDiagnoses,
                diseaseModelProbability);


        RankMaxo rankMaxo = new RankMaxo(hpoToMaxoTermMap, maxoToHpoTermIdMap, maxoHpoTermProbabilities, engine);

        return rankMaxo;
    }

    //TODO: handle possible multiple differential diagnoses with same termId

    /**
     *
     * @param omimIds disease Ids.
     * @param hpoIds hpo Ids.
     * @param hpoTermCounts Map of HPO Term Id and List of HpoFrequency objects.
     * @return List of Frequencies records
     */
    public static List<Frequencies> getFrequencyRecords(Set<TermId> omimIds, Set<TermId> hpoIds,
                                                 Map<TermId, List<HpoFrequency>> hpoTermCounts) {

        List<Frequencies> frequencyRecords = new ArrayList<>();
        //Set<TermId> omimIds = maxoTermScoreRecord.omimTermIds();
        for (TermId hpoId : hpoIds) { //maxoTermScoreRecord.hpoTermIds()
            Map<TermId, Float> maxoFrequencies = new LinkedHashMap<>();
            omimIds.forEach(e -> maxoFrequencies.put(e, null));
            List<HpoFrequency> frequencies = hpoTermCounts.get(hpoId);
            for (HpoFrequency hpoFrequency : frequencies) {
                for (TermId omimId : omimIds) {
                    if (hpoFrequency.omimId().equals(omimId.toString())) {
                        Float frequency = hpoFrequency.frequency();
                        maxoFrequencies.replace(omimId, frequency);
                    }
                }
            }
            frequencyRecords.add(new Frequencies(hpoId, maxoFrequencies.values().stream().toList()));
        }
        return frequencyRecords;
    }

    @Override
    public List<DifferentialDiagnosis> getOrderedDiagnoses(Collection<DifferentialDiagnosis> originalDifferentialDiagnoses,
                                                           RefinementOptions options) {
        if (originalDifferentialDiagnoses.size() < options.nDiseases()) {
            //TODO: replace with MaxodiffRuntimeException that extends RuntimeException.
            throw new RuntimeException("Input No. Diseases larger than No. diseases in sample.");
        }
        List<DifferentialDiagnosis> orderedDiagnoses = originalDifferentialDiagnoses.stream()
                .sorted(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed())
                .toList();

        return orderedDiagnoses.subList(0, options.nDiseases());
    }

    @Override
    public List<HpoDisease> getDiseases(List<DifferentialDiagnosis> differentialDiagnoses) {
        // Get diseaseIds and then diseases from differential diagnoses list
        //TODO: Set of diseaseIds should be a requirement of the Sample, don't need to define it here necessarily.
        Set<TermId> diseaseIds = differentialDiagnoses.stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toSet());
        List<HpoDisease> diseases = new ArrayList<>();
        diseaseIds.forEach(id -> hpoDiseases.diseaseById(id).ifPresent(diseases::add));

        return diseases;
    }

    @Override
    public Map<TermId, List<HpoFrequency>> getHpoTermCounts(List<HpoDisease> diseases) {

        // Get Map of HPO Term Id and List of HpoFrequency objects for list of m diseases.
        Map<TermId, List<HpoFrequency>> hpoTermCountsImmutable = AnalysisUtils.getHpoTermCounts(diseases);

        return new HashMap<>(hpoTermCountsImmutable);
    }

    @Override
    public Map<TermId, Set<TermId>> getMaxoToHpoTermIdMap(List<TermId> termIdsToRemove,
                                                          Map<TermId, List<HpoFrequency>> hpoTermCounts) {


        // Remove HPO terms if desired
        termIdsToRemove.forEach(hpoTermCounts::remove);
//        sample.excludedHpoTermIds().forEach(hpoTermCounts::remove);
        Set<TermId> hpoIds = hpoTermCounts.keySet();

        // Get all the MaXo terms that can be used to diagnose the HPO terms, removing ancestors
        //TODO: make MAXO:HPO term map directly from maxo_diagnostic_annotations.tsv file
        Map<TermId, Set<TermId>> hpoToMaxoTermIdMap = AnalysisUtils.makeHpoToMaxoTermIdMap(fullHpoToMaxoTermIdMap, hpoIds);
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = AnalysisUtils.makeMaxoToHpoTermIdMap(hpo, hpoToMaxoTermIdMap);

        return maxoToHpoTermIdMap;
    }

    /**
     *
     * @param sample Sample info, may or may not be from a phenopacket.
     * @param engine The engine used for the original differential diagnosis calculation (e.g. LIRICAL).
     * @param maxoToHpoTermIdMap Map of MAXO Term Id to Set of HPO Term Ids.
     * @return MAP of DifferentialDiagnosis objects for MAxO term, in reverse score order.
     */
    @Override
    public Map<TermId, List<DifferentialDiagnosis>> getMaxoTermToDifferentialDiagnosesMap(Sample sample,
                                                                                          DifferentialDiagnosisEngine engine,
                                                                                          Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                                                                                          Integer nDiseases) {

        Map<TermId, List<DifferentialDiagnosis>> maxoTermToDifferentialDiagnosesMap = new HashMap<>();
        for (TermId maxoId : maxoToHpoTermIdMap.keySet()) {
            // Get the set of HPO terms that can be ascertained by the MAXO term
            Set<TermId> hpoTermIds = maxoToHpoTermIdMap.get(maxoId);
            List<TermId> maxoDiseaseIds = hpoTermIds.stream().toList();
            System.out.println(maxoId + " ascertained HPO terms: " + maxoDiseaseIds);
            Sample maxoSample = Sample.of(sample.id(), maxoDiseaseIds, sample.excludedHpoTermIds());
            List<DifferentialDiagnosis> maxoDiagnoses = engine.run(maxoSample);
            List<DifferentialDiagnosis> orderedMaxoDiagnoses = maxoDiagnoses.stream()
                    .sorted(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed())
                    .toList();
            maxoTermToDifferentialDiagnosesMap.put(maxoId, orderedMaxoDiagnoses.subList(0, nDiseases));
        }

        return maxoTermToDifferentialDiagnosesMap;
    }

    @Override
    public HpoDiseases getHPOADiseases() {
        return this.hpoDiseases;
    }

}
