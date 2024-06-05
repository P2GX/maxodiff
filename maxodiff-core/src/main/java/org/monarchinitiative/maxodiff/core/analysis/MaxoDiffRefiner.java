package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosisModel;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;

public class MaxoDiffRefiner implements DiffDiagRefiner {

    private final HpoDiseases hpoDiseases;
    private final Map<TermId, Set<TermId>> fullHpoToMaxoTermMap;
    private final MinimalOntology hpo;

    public MaxoDiffRefiner(HpoDiseases hpoDiseases, Map<TermId, Set<TermId>> fullHpoToMaxoTermMap,
                           MinimalOntology hpo) {
        this.hpoDiseases = hpoDiseases;
        this.fullHpoToMaxoTermMap = fullHpoToMaxoTermMap;
        this.hpo = hpo;
    }


    @Override
    public RefinementResults run(Sample sample, RefinementOptions options) {
        if (sample.differentialDiagnoses().size() < options.nDiseases()) {
            //TODO: replace with MaxodiffRuntimeException that extends RuntimeException.
            throw new RuntimeException("Input No. Diseases larger than No. diseases in sample.");
        }
        List<DifferentialDiagnosisModel> orderedDiagnoses = sample.differentialDiagnoses().stream()
                .sorted(Comparator.comparingDouble(DifferentialDiagnosisModel::score).reversed()).toList();
        List<DifferentialDiagnosisModel> differentialDiagnoses = orderedDiagnoses.subList(0, options.nDiseases());

        // Get diseaseIds and then diseases from differential diagnoses list
        //TODO: Set of diseaseIds should be a requirement of the Sample, don't need to define it here necessarily.
        Set<TermId> diseaseIds = differentialDiagnoses.stream()
                .map(DifferentialDiagnosisModel::diseaseId)
                .collect(Collectors.toSet());
        List<HpoDisease> diseases = new ArrayList<>();
        diseaseIds.forEach(id -> hpoDiseases.diseaseById(id).ifPresent(diseases::add));

        // Get Map of HPO Term Id and List of HpoFrequency objects for list of m diseases.
        Map<TermId, List<HpoFrequency>> hpoTermCountsImmutable = getHpoTermCounts(diseases);
        Map<TermId, List<HpoFrequency>> hpoTermCounts = new HashMap<>(hpoTermCountsImmutable);

        // Remove HPO terms present in the sample
        sample.presentHpoTermIds().forEach(hpoTermCounts::remove);
        sample.excludedHpoTermIds().forEach(hpoTermCounts::remove);
        Set<TermId> hpoIds = hpoTermCounts.keySet();

        // Get all the MaXo terms that can be used to diagnose the HPO terms, removing ancestors
        //TODO: make MAXO:HPO term map directly from maxo_diagnostic_annotations.tsv file
        Map<TermId, Set<TermId>> hpoToMaxoTermIdMap = makeHpoToMaxoTermIdMap(fullHpoToMaxoTermMap, hpoIds);
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = makeMaxoToHpoTermIdMap(hpo, hpoToMaxoTermIdMap);

        // Calculate final scores and make list of MaxodiffResult objects.
        List<MaxodiffResult> maxodiffResultsList = new ArrayList<>();
        for (TermId maxoId : maxoToHpoTermIdMap.keySet()) {
            // Get the set of HPO terms that can be ascertained by the MAXO term
            Set<TermId> hpoTermIds = maxoToHpoTermIdMap.get(maxoId);
            // Get HPO term combinations
            List<List<TermId>> hpoCombos = getHpoTermCombos(maxoToHpoTermIdMap, maxoId);
            // Calculate final score and make score record
            MaxoTermScore maxoTermScore = getMaxoTermScoreRecord(hpoTermIds,
                    hpoCombos,
                    maxoId,
                    differentialDiagnoses,
                    diseases,
                    options);
            // Get HPO frequency records
            List<Frequencies> frequencies = getFrequencyRecords(maxoTermScore, hpoTermCounts);
            // Make MaxodiffResult for the MAXO term
            MaxodiffResult maxodiffResult = new MaxodiffResultImpl(maxoTermScore, frequencies);
            maxodiffResultsList.add(maxodiffResult);
        }

        // Sort MaxodiffResult list in descending order of score difference.
        maxodiffResultsList.sort((a, b) -> b.maxoTermScore().scoreDiff().compareTo(a.maxoTermScore().scoreDiff()));

        // Return RefinementResults object, which contains the list of MaxodiffResult objects.
        return new RefinementResultsImpl(maxodiffResultsList);
    }

    //TODO: handle possible multiple differential diagnoses with same termId

    /**
     * Calculate relative disease difference value for a target disease.
     * @param differentialDiagnoses List of DifferentialDiagnosis objects sorted in order of descending score.
     * @param i Index of the target disease in the differential diagnosis list.
     * @return
     */
    private static double getRelativeDiseaseDiffValue(List<DifferentialDiagnosisModel> differentialDiagnoses, int i) {
        double sum = 0.0;
        //TODO: Do we use the last item (disease with the lowest posttest prob in list of m diseases) when getting sublist?
        double targetLR = differentialDiagnoses.get(i).lr();
        for (DifferentialDiagnosisModel dd : differentialDiagnoses.subList(i, differentialDiagnoses.size())) {
            double lr = dd.lr();
            sum += targetLR / lr;
        }
        return sum;
    }

    private static double calculateRelDiseaseDiffSum(List<DifferentialDiagnosisModel> differentialDiagnoses) {
        double sum = 0.0;
        for(int i=0; i<differentialDiagnoses.size(); i++) {
            sum += getRelativeDiseaseDiffValue(differentialDiagnoses, i);
        }
        return sum;
    }

    /**
     *
     * @param diseases List of Hpo diseases
     * @return Map of HPO Term Id and List of HpoFrequency objects.
     */
    private static Map<TermId, List<HpoFrequency>> getHpoTermCounts(List<HpoDisease> diseases) {
        // Collect HPO terms and frequencies for the target m diseases
        DiseaseTermCount diseaseTermCount = DiseaseTermCount.of(diseases);
        return diseaseTermCount.hpoTermCounts();
    }

    /**
     *
     * @param fullHpoToMaxoTermMap Map of all HPO -> MAXO TermId set mappings from maxo_diagnostic_annotations file.
     * @param hpoTermIds Set of HPO TermIds associated with the subset of m diseases.
     * @return Map of HPO -> MAXO TermId set mappings for the subset of m diseases.
     */
    private static Map<TermId, Set<TermId>> makeHpoToMaxoTermIdMap(Map<TermId, Set<TermId>> fullHpoToMaxoTermMap,
                                                                  Set<TermId> hpoTermIds) {
        Map<TermId, Set<TermId>> hpoToMaxoTermIdMap = new HashMap<>();
        for (TermId hpoId : hpoTermIds) {
            for (TermId hpoTermId : fullHpoToMaxoTermMap.keySet()) {
                if (hpoTermId.equals(hpoId)) {
                    Set<TermId> maxoTermIds = fullHpoToMaxoTermMap.get(hpoTermId);
                    hpoToMaxoTermIdMap.put(hpoTermId, maxoTermIds);
                    break;
                }
            }
        }
        return hpoToMaxoTermIdMap;
    }

    /**
     *
     * @param ontology HPO Ontology.
     * @param hpoToMaxoTermMap Map of HPO -> MAXO TermId set mappings for the subset of m diseases.
     * @return Map of MAXO -> HPO TermId set mappings for the subset of m diseases. HPO ancestors are removed.
     */
    private static Map<TermId, Set<TermId>> makeMaxoToHpoTermIdMap(MinimalOntology ontology, Map<TermId, Set<TermId>> hpoToMaxoTermMap) {
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = new HashMap<>();
        for (Map.Entry<TermId, Set<TermId>> entry : hpoToMaxoTermMap.entrySet()) {
            TermId hpoTermId = entry.getKey();
            Set<TermId> maxoTermIds = entry.getValue();
            for (TermId maxoTermId : maxoTermIds) {
                if (!maxoToHpoTermIdMap.containsKey(maxoTermId)) {
                    maxoToHpoTermIdMap.put(maxoTermId, new HashSet<>(Collections.singleton(hpoTermId)));
                } else {
                    Set<TermId> hpoTermIds = maxoToHpoTermIdMap.get(maxoTermId);
                    hpoTermIds.add(hpoTermId);
                    maxoToHpoTermIdMap.replace(maxoTermId, hpoTermIds);
                }
            }
        }
        //TODO: removing ancestors possibly incorrect for excluded HPO features
        for (Map.Entry<TermId, Set<TermId>> e : maxoToHpoTermIdMap.entrySet()) {
            // Remove HPO ancestor term Ids from list
            TermId mId = e.getKey();
            Set<TermId> hpoIdSet = new HashSet<>(e.getValue());
            for (TermId hpoId : e.getValue()) {
                for (TermId ancestor : ontology.graph().getAncestors(hpoId)) {
                    hpoIdSet.remove(ancestor);
                }
            }
            maxoToHpoTermIdMap.replace(mId, hpoIdSet);
        }
        return maxoToHpoTermIdMap;
    }

    /**
     *
     * @param maxoToHpoTermIdMap Map of MAXO -> HPO TermId set mappings for the subset of m diseases. HPO ancestors are removed.
     * @param maxoId Term Id for the MAXO Term to get HPO Combos for.
     * @return List of combinations of HPO term Ids
     * (e.g. given input [A, B, C] yields [[A], [B], [A, B], [C], [A, C], [B, C], [A, B, C]])
     */
    private static List<List<TermId>> getHpoTermCombos(Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                                                       TermId maxoId) {

        // Collect HPO terms that can be ascertained by MAXO term
        List<TermId> hpoTermIds = maxoToHpoTermIdMap.get(maxoId).stream().toList();
        // Get HPO term combos for HPO terms ascertained by MAXO term
        return DifferentialDiagnosis.getHpoTermCombos(hpoTermIds);
    }

    /**
     *
     * @param hpoCombo List of HPO term Ids in combo.
     * @param diseases List of HPODisease objects for the target m diseases.
     * @return Set of disease OMIM Ids associated with the HPO term combo.
     */
    private static Set<TermId> getHpoComboAssociatedDiseaseIds(List<TermId> hpoCombo,
                                                               List<HpoDisease> diseases) {

        // Get list of disease OMIM Ids associated with HPO term combo
        Set<TermId> omimIds = new HashSet<>();
        for (HpoDisease disease : diseases) {
            //TODO: disease.annotationTermIdList() is also used in DiseaseTermCount.
            // Reformat this to work with hpoTermCounts instead?
            List<TermId> annotationHpoIds = disease.annotationTermIdList();
            for (TermId hpoId : hpoCombo) {
                if (annotationHpoIds.contains(hpoId)) {
                    omimIds.add(disease.id());
                }
            }
        }
        return omimIds;
    }

    /**
     *
     * @param differentialDiagnoses List of DifferentialDiagnosis objects sorted in order of descending score.
     * @param diseases List of HPODisease objects for the target m diseases.
     * @param hpoCombos List of combinations of HPO term Ids for the associated MAXO term.
     * @param weight Weight parameter to use in the final score calculation.
     * @return Final score for the MAXO term.
     */
    private static double calculateMaxoTermFinalScore(List<DifferentialDiagnosisModel> differentialDiagnoses,
                                                      List<HpoDisease> diseases,
                                                      List<List<TermId>> hpoCombos,
                                                      double weight) {

        List<Double> comboScores = new ArrayList<>();
        for (List<TermId> hpoCombo : hpoCombos) {
            Set<TermId> omimIds = getHpoComboAssociatedDiseaseIds(hpoCombo, diseases);
            // Calculate S using HPO combo associated disease OMIM Ids
            List<DifferentialDiagnosisModel> comboDiagnoses = new ArrayList<>();
            for (TermId omimId : omimIds) {
                for (DifferentialDiagnosisModel diagnosisModel : differentialDiagnoses) {
                    if (omimId.equals(diagnosisModel.diseaseId())) {
                        comboDiagnoses.add(diagnosisModel);
                    }
                }
            }
            double scoreSum = comboDiagnoses.stream().mapToDouble(DifferentialDiagnosisModel::score).sum();
            double relativeDiseaseDiffSum = calculateRelDiseaseDiffSum(comboDiagnoses);
            double comboFinalScore = weight * scoreSum + (1 - weight) * relativeDiseaseDiffSum;
            comboScores.add(comboFinalScore);
        }
        // Take the mean of the HPO combo scores as the final score for the MAXO term
        OptionalDouble maxoFinalScoreOptional = comboScores.stream().mapToDouble(s -> s).average();
        double maxoFinalScore = 0.0;
        if (maxoFinalScoreOptional.isPresent()) {
            maxoFinalScore = maxoFinalScoreOptional.getAsDouble();
        }
        return maxoFinalScore;
    }

    /**
     *
     * @param hpoTermIds Set of HPO terms that can be ascertained by the MAXO term.
     * @param hpoCombos Combinations of HPO terms in hpoTermIds set.
     * @param maxoId TermId of MAXO Term.
     * @param differentialDiagnoses List of DifferentialDiagnosis objects sorted in order of descending score.
     * @param diseases List of HPODisease objects for the target m diseases.
     * @param options Refinement options.
     * @return MaxoTermScore record.
     */
    private static MaxoTermScore getMaxoTermScoreRecord(Set<TermId> hpoTermIds,
                                                     List<List<TermId>> hpoCombos,
                                                     TermId maxoId,
                                                     List<DifferentialDiagnosisModel> differentialDiagnoses,
                                                     List<HpoDisease> diseases,
                                                     RefinementOptions options) {

        //TODO: check if logic for getting initialScore is correct.
        double maxoTermInitialScore = calculateMaxoTermFinalScore(differentialDiagnoses,
                diseases,
                hpoCombos,
                1.0);
        double maxoTermFinalScore = calculateMaxoTermFinalScore(differentialDiagnoses,
                diseases,
                hpoCombos,
                options.weight());
        double scoreDiff = maxoTermFinalScore - maxoTermInitialScore;

        Set<TermId> diseaseIds = new LinkedHashSet<>();
        List<DifferentialDiagnosisModel> differentialDiagnosisModels = new ArrayList<>(differentialDiagnoses);
        differentialDiagnosisModels.sort(Comparator.comparingDouble(DifferentialDiagnosisModel::score).reversed());
        differentialDiagnosisModels.forEach(d -> diseaseIds.add(d.diseaseId()));
        int nHpoTerms = hpoTermIds.size();

        return new MaxoTermScore(maxoId.toString(), null, options.nDiseases(),
                diseaseIds, nHpoTerms, hpoTermIds, null,
                maxoTermInitialScore, maxoTermFinalScore, scoreDiff);
    }

    /**
     *
     * @param maxoTermScoreRecord MaxoTermScore record.
     * @param hpoTermCounts Map of HPO Term Id and List of HpoFrequency objects.
     * @return List of Frequencies records
     */
    private static List<Frequencies> getFrequencyRecords(MaxoTermScore maxoTermScoreRecord,
                                                 Map<TermId, List<HpoFrequency>> hpoTermCounts) {

        List<Frequencies> frequencyRecords = new ArrayList<>();
        Set<TermId> omimIds = maxoTermScoreRecord.omimTermIds();
        for (TermId hpoId : maxoTermScoreRecord.hpoTermIds()) {
            String hpoLabel = null;
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
            frequencyRecords.add(new Frequencies(hpoId, hpoLabel, maxoFrequencies.values().stream().toList()));
        }
        return frequencyRecords;
    }

}
