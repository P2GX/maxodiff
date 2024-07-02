package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

class AnalysisUtils {

    private AnalysisUtils() {}

    /**
     *
     * @param diseases List of Hpo diseases
     * @return Map of HPO Term Id and List of HpoFrequency objects.
     */
    static Map<TermId, List<HpoFrequency>> getHpoTermCounts(List<HpoDisease> diseases) {
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
    static Map<TermId, Set<TermId>> makeHpoToMaxoTermIdMap(Map<TermId, Set<TermId>> fullHpoToMaxoTermMap,
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
    static Map<TermId, Set<TermId>> makeMaxoToHpoTermIdMap(MinimalOntology ontology, Map<TermId, Set<TermId>> hpoToMaxoTermMap) {
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
                try {
                    for (TermId ancestor : ontology.graph().getAncestors(hpoId)) {
                        hpoIdSet.remove(ancestor);
                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
            maxoToHpoTermIdMap.replace(mId, hpoIdSet);
        }
        return maxoToHpoTermIdMap;
    }

    /**
     * Calculate relative disease difference value for a target disease.
     * @param differentialDiagnoses List of DifferentialDiagnosis objects sorted in order of descending score.
     * @param i Index of the target disease in the differential diagnosis list.
     * @return
     */
    // Eq 2,3
    private static double getRelativeDiseaseDiffValue(List<DifferentialDiagnosis> differentialDiagnoses, int i) {
        double sum = 0.0;
        //TODO: Do we use the last item (disease with the lowest posttest prob in list of m diseases) when getting sublist?
        double targetLR = differentialDiagnoses.get(i).lr();
        for (DifferentialDiagnosis dd : differentialDiagnoses.subList(i /*i+1*/, differentialDiagnoses.size())) {
            double lr = dd.lr();
            sum += targetLR / lr;
        }
        return sum;
    }

    // Eq 4
    private static double calculateRelDiseaseDiffSum(List<DifferentialDiagnosis> differentialDiagnoses) {
        double sum = 0.0;
        for(int i=0; i<differentialDiagnoses.size() /*-1*/; i++) {
            sum += getRelativeDiseaseDiffValue(differentialDiagnoses, i);
        }
        return sum;
    }

    /**
     *
     * @param maxoToHpoTermIdMap Map of MAXO -> HPO TermId set mappings for the subset of m diseases. HPO ancestors are removed.
     * @param maxoId Term Id for the MAXO Term to get HPO Combos for.
     * @return List of combinations of HPO term Ids
     * (e.g. given input [A, B, C] yields [[A], [B], [A, B], [C], [A, C], [B, C], [A, B, C]])
     */
    static List<List<TermId>> getHpoTermCombos(Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                                               TermId maxoId) {

        // Collect HPO terms that can be ascertained by MAXO term
        List<TermId> hpoTermIds = maxoToHpoTermIdMap.get(maxoId).stream().toList();
        // Get HPO term combos for HPO terms ascertained by MAXO term
        List<List<TermId>> hpoComboList = new ArrayList<>();
        // Start i at 1, so that we don't include the empty set in the results
        for (long i = 1; i < Math.pow(2, hpoTermIds.size()); i++) {
            List<TermId> hpoIdList = new ArrayList<>();
            for (int j = 0; j < hpoTermIds.size(); j++) {
                if ((i & (long) Math.pow(2, j)) > 0) {
                    // Include j in list
                    hpoIdList.add(hpoTermIds.get(j));
                }
            }
            hpoComboList.add(hpoIdList);
            // Cap the number of combos at 32
            //TODO: sample up to 32 random combos
            if (hpoComboList.size() == 32) {
                break;
            }
        }
        return hpoComboList;
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
    private static double calculateMaxoTermFinalScore(List<DifferentialDiagnosis> differentialDiagnoses,
                                                      List<HpoDisease> diseases,
                                                      List<List<TermId>> hpoCombos,
                                                      double weight) {

        List<Double> comboScores = new ArrayList<>();
        for (List<TermId> hpoCombo : hpoCombos) {
            Set<TermId> omimIds = getHpoComboAssociatedDiseaseIds(hpoCombo, diseases);
            // Calculate S using HPO combo associated disease OMIM Ids
            List<DifferentialDiagnosis> comboDiagnoses = new ArrayList<>();
            for (TermId omimId : omimIds) {
                for (DifferentialDiagnosis diagnosisModel : differentialDiagnoses) {
                    if (omimId.equals(diagnosisModel.diseaseId())) {
                        comboDiagnoses.add(diagnosisModel);
                    }
                }
            }
            double scoreSum = comboDiagnoses.stream().mapToDouble(DifferentialDiagnosis::score).sum();
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
    static MaxoTermScore getMaxoTermScoreRecord(Set<TermId> hpoTermIds,
                                                List<List<TermId>> hpoCombos,
                                                TermId maxoId,
                                                List<DifferentialDiagnosis> differentialDiagnoses,
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
        List<DifferentialDiagnosis> differentialDiagnosisModels = new ArrayList<>(differentialDiagnoses);
        differentialDiagnosisModels.sort(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed());
        differentialDiagnosisModels.forEach(d -> diseaseIds.add(d.diseaseId()));
        int nHpoTerms = hpoTermIds.size();

        return new MaxoTermScore(maxoId.toString(), options.nDiseases(),
                diseaseIds, nHpoTerms, hpoTermIds,
                maxoTermInitialScore, maxoTermFinalScore, scoreDiff);
    }
}
