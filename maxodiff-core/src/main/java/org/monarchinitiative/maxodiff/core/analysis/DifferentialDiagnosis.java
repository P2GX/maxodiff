package org.monarchinitiative.maxodiff.core.analysis;

import org.apache.commons.math3.random.EmpiricalDistribution;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.io.MaxodiffBuilder;
import org.monarchinitiative.maxodiff.core.io.MaxodiffDataException;
import org.monarchinitiative.maxodiff.core.io.MaxodiffDataResolver;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class DifferentialDiagnosis {

    //Functions using results from a LIRICAL calculation in the same session
    /**
     *
     * @param results {@link AnalysisResults}. LIRICAL analysis results.
     * @param diseaseIds List<TermId>. List of disease Ids to use for differential diagnosis calculation.
     * @return Map<TermId, Double>. Map of the posttest probabilities for the target m diseases.
     */
    public static Map<TermId, Double> posttestProbabilityMap(AnalysisResults results, List<TermId> diseaseIds) {
        Map<TermId, Double> probabilityMap = new LinkedHashMap<>();
        for (TermId id : diseaseIds) {
            var result = results.resultByDiseaseId(id);
            result.ifPresent(testResult -> probabilityMap.put(id, testResult.posttestProbability()));
        }
        return probabilityMap;
    }

    /**
     *
     * @param results {@link AnalysisResults}. LIRICAL analysis results.
     * @param diseaseIds List<TermId>. List of disease Ids to use for differential diagnosis calculation.
     * @param targetDisease TermId. Target disease from the phenopacket.
     * @return double. Sum of the target disease LR / disease list disease LR.
     */
    public static double relativeDiseaseDiff(AnalysisResults results, List<TermId> diseaseIds, TermId targetDisease) {
        double sum = 0.0;
        List<TestResult> orderedResults = results.resultsWithDescendingPostTestProbability().toList();
        var targetResultOptional = orderedResults.stream().filter(result -> result.diseaseId().equals(targetDisease)).findFirst();
        if (targetResultOptional.isPresent()) {
            TestResult targetResult = targetResultOptional.get();
            int targetResultIdx = orderedResults.indexOf(targetResult);
            List<TestResult> resultsSublist = orderedResults.subList(0, targetResultIdx);
            List<TestResult> diffResultsList = resultsSublist.stream().filter(res -> diseaseIds.contains(res.diseaseId()))
                    .sorted(Comparator.comparingDouble(TestResult::posttestProbability).reversed()).toList();
            List<Double> diffLRList = diffResultsList.stream().map(res -> res.observedResults().get(0).lr()).toList();
            double targetLR = targetResult.observedResults().get(0).lr();
            for (double lr : diffLRList)
                sum += targetLR / lr;
        }
        return sum;
    }

    //Functions using results from a separate LIRICAL output file
    /**
     *
     * @param liricalOutputRecords List<LiricalResultsFileRecord>. List of {@link LiricalResultsFileRecord} results from LIRICAL output file.
     * @param diseaseIds List<TermId>. List of disease Ids to use for differential diagnosis calculation.
     * @return Map<TermId, Double>. Map of the posttest probabilities for the target m diseases.
     */
    public static Map<TermId, Double> posttestProbabilityMap(List<LiricalResultsFileRecord> liricalOutputRecords,
                                                      List<TermId> diseaseIds) {
        Map<TermId, Double> probabilityMap = new LinkedHashMap<>();
        for (TermId id : diseaseIds) {
            var liricalRecord = liricalOutputRecords.stream().filter(r -> r.omimId().equals(id)).findFirst();
            liricalRecord.ifPresent(liricalResultsFileRecord -> probabilityMap.put(id, liricalResultsFileRecord.posttestProbability()));
        }
        return probabilityMap;
    }

    /**
     *
     * @param liricalOutputRecords List<LiricalResultsFileRecord>. List of {@link LiricalResultsFileRecord} results from LIRICAL output file.
     * @param diseaseIds List<TermId>. List of disease Ids to use for differential diagnosis calculation.
     * @param targetDisease TermId. Target disease from the phenopacket.
     * @return double. Sum of the target disease LR / disease list disease LR.
     */
    public static double relativeDiseaseDiff(List<LiricalResultsFileRecord> liricalOutputRecords,
                                             List<TermId> diseaseIds, TermId targetDisease) {
        double sum = 0.0;
        List<LiricalResultsFileRecord> orderedResults = liricalOutputRecords.stream()
                .sorted(Comparator.comparingDouble(LiricalResultsFileRecord::posttestProbability).reversed()).toList();
        var targetResultOptional = orderedResults.stream().filter(result -> result.omimId().equals(targetDisease)).findFirst();
        if (targetResultOptional.isPresent()) {
            LiricalResultsFileRecord targetResult = targetResultOptional.get();
            int targetResultIdx = orderedResults.indexOf(targetResult);
            List<LiricalResultsFileRecord> resultsSublist = orderedResults.subList(0, targetResultIdx);
            List<LiricalResultsFileRecord> diffResultsList = resultsSublist.stream().filter(res -> diseaseIds.contains(res.omimId()))
                    .sorted(Comparator.comparingDouble(LiricalResultsFileRecord::posttestProbability).reversed()).toList();
            //TODO: Double check if Likelihood Ratio from LIRICAL is actually Log(LR)
            List<Double> diffLRList = diffResultsList.stream().map(LiricalResultsFileRecord::likelihoodRatio).toList();
            double targetLR = targetResult.likelihoodRatio();
            for (double lr : diffLRList)
                sum += targetLR / lr;
        }
        return sum;
    }

    /**
     *
     * @param results {@link AnalysisResults}. LIRICAL analysis results.
     * @param liricalOutputRecords List<LiricalResultsFileRecord>. List of {@link LiricalResultsFileRecord} results from LIRICAL output file.
     * @param diseaseIds List<TermId>. List of disease Ids to use for differential diagnosis calculation.
     * @return double. Sum of relativeDiseaseDiff scores.
     */
    public static double scoreSum(AnalysisResults results, List<LiricalResultsFileRecord> liricalOutputRecords,
                                  List<TermId> diseaseIds) {
        double sum = 0.0;
        List<TermId> subIds = diseaseIds.subList(0, diseaseIds.size());
        for (TermId diseaseId : subIds)
            if (results != null) {
                sum += relativeDiseaseDiff(results, diseaseIds, diseaseId);
            } else if (liricalOutputRecords != null) {
                sum += relativeDiseaseDiff(liricalOutputRecords, diseaseIds, diseaseId);
            }
        return sum;
    }

    /**
     *
     * @param results {@link AnalysisResults}. LIRICAL analysis results.
     * @param liricalOutputRecords List<LiricalResultsFileRecord>. List of {@link LiricalResultsFileRecord} results from LIRICAL output file.
     * @param diseaseIds List<TermId>. List of disease Ids to use for differential diagnosis calculation.
     * @param weight double. Weight value to use in the differential diagnosis calculation.
     * @return double. Final differential diagnosis calculation score.
     */
    public static double finalScore(AnalysisResults results, List<LiricalResultsFileRecord> liricalOutputRecords,
                                    List<TermId> diseaseIds, double weight) {
        List<Double> probabilities;
        double p = 0;
        double q = 0;
        if (results != null) {
            probabilities = posttestProbabilityMap(results, diseaseIds).values().stream().toList();
            p = probabilities.stream().mapToDouble(Double::doubleValue).sum();
            q = scoreSum(results, null, diseaseIds);
        } else if (liricalOutputRecords != null) {
            probabilities = posttestProbabilityMap(liricalOutputRecords, diseaseIds).values().stream().toList();
            p = probabilities.stream().mapToDouble(Double::doubleValue).sum();
            q = scoreSum(null, liricalOutputRecords, diseaseIds);
        }

        return weight*p + (1-weight)*q;
    }

    /**
     *
     * @param dataResolver {@link MaxodiffDataResolver}. Class for loading maxodiff data files.
     * @param diseaseIds List<TermId>. List of disease Ids to use for the differential diagnosis calculation.
     * @return List<HpoDisease>. List of {@link HpoDisease} diseases to use for the differential diagnosis calculation.
     * @throws MaxodiffDataException
     */
    public static List<HpoDisease> makeDiseaseList(MaxodiffDataResolver dataResolver, List<TermId> diseaseIds) throws MaxodiffDataException {
        List<HpoDisease> diseases = new ArrayList<>();
        Path annotPath = dataResolver.phenotypeAnnotations();
        Ontology hpo = MaxodiffBuilder.loadOntology(dataResolver.hpoJson());
        HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.defaultOptions();
        HpoDiseases hpoDiseases = MaxodiffBuilder.loadHpoDiseases(annotPath, hpo, options);
        diseaseIds.forEach(id -> hpoDiseases.diseaseById(id).ifPresent(diseases::add));
        return diseases;
    }

    /**
     *
     * @param fullHpoToMaxoTermMap Map<SimpleTerm, Set<SimpleTerm>>. Map of all HPO terms to Set of MaXo terms that can be used to diagnose HPO terms.
     * @param hpoTermIds Set<TermId>. Set of HPO term Ids associated with the diseases used in the differential diagnosis calculation.
     * @return Map<SimpleTerm, Set<SimpleTerm>>. Map of HPO terms to Set of associated MaXo terms.
     */
    public static Map<SimpleTerm, Set<SimpleTerm>> makeHpoToMaxoTermMap(Map<SimpleTerm, Set<SimpleTerm>> fullHpoToMaxoTermMap,
                                                                 Set<TermId> hpoTermIds) {
        Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap = new HashMap<>();
        for (TermId hpoId : hpoTermIds) {
            for (SimpleTerm hpoTerm : fullHpoToMaxoTermMap.keySet()) {
                if (hpoTerm.tid().equals(hpoId)) {
                    Set<SimpleTerm> maxoTerms = fullHpoToMaxoTermMap.get(hpoTerm);
                    hpoToMaxoTermMap.put(hpoTerm, maxoTerms);
                    break;
                }
            }
        }
        return hpoToMaxoTermMap;
    }

    /**
     *
     * @param ontology Ontology. Hpo ontology.
     * @param hpoToMaxoTermMap Map<SimpleTerm, Set<SimpleTerm>>. Map of HPO terms to Set of associated MaXo terms.
     * @return Map<SimpleTerm, Set<SimpleTerm>>. Map of MaXo terms to Set of associated HPO terms, not including ancestors.
     */
    public static Map<SimpleTerm, Set<SimpleTerm>> makeMaxoToHpoTermMap(Ontology ontology, Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap) {
        Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoTermMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : hpoToMaxoTermMap.entrySet()) {
            SimpleTerm hpoTerm = entry.getKey();
            Set<SimpleTerm> maxoTerms = entry.getValue();
            for (SimpleTerm maxoTerm : maxoTerms) {
                if (!maxoToHpoTermMap.containsKey(maxoTerm)) {
                    maxoToHpoTermMap.put(maxoTerm, new HashSet<>(Collections.singleton(hpoTerm)));
                } else {
                    Set<SimpleTerm> hpoTerms = maxoToHpoTermMap.get(maxoTerm);
                    hpoTerms.add(hpoTerm);
                    maxoToHpoTermMap.replace(maxoTerm, hpoTerms);
                }
            }
        }
        for (SimpleTerm mTerm : maxoToHpoTermMap.keySet()) {
            // Remove HPO ancestor term Ids from list
            Set<SimpleTerm> allHpoTerms = maxoToHpoTermMap.get(mTerm);
            Set<TermId> hpoIdSet = new HashSet<>();
            allHpoTerms.forEach(t -> hpoIdSet.add(t.tid()));
            Set<TermId> ancestors = OntologyAlgorithm.getAncestorTerms(ontology, hpoIdSet, false);
            hpoIdSet.removeAll(ancestors);
            Set<SimpleTerm> hpoTermSet = allHpoTerms.stream().filter(hpoTerm -> hpoIdSet.contains(hpoTerm.tid())).collect(Collectors.toSet());
            maxoToHpoTermMap.replace(mTerm, hpoTermSet);
        }
        return maxoToHpoTermMap;
    }

    /**
     *
     * @param hpoTermIds List<TermId>. List of HPO term Ids.
     * @return List<List<TermId>>. List of combinations of HPO term Ids
     * (e.g. given input [A, B, C] yields [[A], [B], [A, B], [C], [A, C], [B, C], [A, B, C]])
     */
    public static List<List<TermId>> getHpoTermCombos(List<TermId> hpoTermIds) {

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
            if (hpoComboList.size() == 32) {
                break;
            }
        }
        return hpoComboList;
    }

    /**
     *
     * @param maxoToHpoTermMap Map<SimpleTerm, Set<SimpleTerm>>. Map of MaXo terms to Set of associated HPO terms, not including ancestors.
     * @param diseases List<HpoDisease>. List of {@link HpoDisease} diseases to use for the differential diagnosis calculation.
     * @param results {@link AnalysisResults}. LIRICAL analysis results.
     * @param liricalOutputRecords List<LiricalResultsFileRecord>. List of {@link LiricalResultsFileRecord} results from LIRICAL output file.
     * @param weight double. Weight value to use in the differential diagnosis calculation.
     * @return Map<SimpleTerm, Double>. Map of MaXo terms to final differential diagnosis scores.
     */
    public static Map<SimpleTerm, Double> makeMaxoScoreMap(Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoTermMap, List<HpoDisease> diseases,
                                                       AnalysisResults results, List<LiricalResultsFileRecord> liricalOutputRecords, double weight) {
        Map<SimpleTerm, Double> maxoScoreMap = new HashMap<>();
        for (SimpleTerm maxoTerm : maxoToHpoTermMap.keySet()) {
            maxoScoreMap.put(maxoTerm, 0.0);
            // Collect HPO terms that can be ascertained by MAxO term
            List<SimpleTerm> hpoTerms = maxoToHpoTermMap.get(maxoTerm).stream().toList();
            List<TermId> hpoTermIds = new ArrayList<>();
            hpoTerms.forEach(t -> hpoTermIds.add(t.tid()));
            // Calculate differential diagnosis score, S, for HPO term combos
            List<List<TermId>> hpoCombos = getHpoTermCombos(hpoTermIds);
            List<Double> finalScores = new ArrayList<>();
            for (List<TermId> hpoCombo : hpoCombos) {
                // Get list of disease OMIM Ids associated with HPO term combo
                Set<TermId> omimIds = new HashSet<>();
                for (HpoDisease disease : diseases) {
                    List<TermId> annotationHpoIds = disease.annotationTermIdList();
                    for (TermId hpoId : hpoCombo) {
                        if (annotationHpoIds.contains(hpoId)) {
                            omimIds.add(disease.id());
                        }
                    }
                }
                // Calculate S using associated disease OMIM Ids
                double comboFinalScore = finalScore(results, liricalOutputRecords, omimIds.stream().toList(), weight);
                finalScores.add(comboFinalScore);
            }
            // Add max mean final score to map
            OptionalDouble maxoFinalScoreOptional = finalScores.stream().mapToDouble(s -> s).average();
            if (maxoFinalScoreOptional.isPresent()) {
                double maxoFinalScore = maxoFinalScoreOptional.getAsDouble();
                if (maxoFinalScore > maxoScoreMap.get(maxoTerm)) {
                    maxoScoreMap.replace(maxoTerm, maxoFinalScore);
                }
            }
        }
        return maxoScoreMap;
    }

    /**
     *
     * @param maxoScores double[]. Array of differential diagnosis scores.
     * @return List<Double>. List of Empirical Cumulative Distribution probability values.
     */
    public static List<Double> getScoreCumulativeDistribution(double[] maxoScores) {
        List<Double> scoreCumulativeDistributionList = new ArrayList<>();
        int nScores = maxoScores.length;
        int binCount = nScores/10;
        EmpiricalDistribution empiricalDistribution = new EmpiricalDistribution(binCount);
        empiricalDistribution.load(maxoScores);
        for (double maxoScore : maxoScores) {
            scoreCumulativeDistributionList.add(empiricalDistribution.cumulativeProbability(maxoScore));
        }
        return scoreCumulativeDistributionList;
    }

}
