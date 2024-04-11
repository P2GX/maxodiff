package org.monarchinitiative.maxodiff.core;

import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.maxodiff.core.io.MaxodiffBuilder;
import org.monarchinitiative.maxodiff.core.io.MaxodiffDataException;
import org.monarchinitiative.maxodiff.core.io.MaxodiffDataResolver;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.util.*;

public class DifferentialDiagnosis {

    public double posttestProbabilitySum(AnalysisResults results, List<TermId> diseaseIds) {
        double sum = 0.0;
        for (TermId id : diseaseIds) {
            var result = results.resultByDiseaseId(id);
            if (result.isPresent())
                sum += result.get().posttestProbability();
        }
        return sum / 100;
    }

    public double relativeDiseaseDiff(AnalysisResults results, List<TermId> diseaseIds, TermId targetDisease) {
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

    public double scoreSum(AnalysisResults results, List<TermId> diseaseIds) {
        double sum = 0.0;
        List<TermId> subIds = diseaseIds.subList(0, diseaseIds.size());
        for (TermId diseaseId : subIds)
            sum += relativeDiseaseDiff(results, diseaseIds, diseaseId);
        return sum;
    }

    public double finalScore(AnalysisResults results, List<TermId> diseaseIds, double weight) {
        double p = posttestProbabilitySum(results, diseaseIds);
        double q = scoreSum(results, diseaseIds);
        return weight*p + (1-weight)*q;
    }

    public List<HpoDisease> makeDiseaseList(MaxodiffDataResolver dataResolver, List<TermId> diseaseIds) throws MaxodiffDataException {
        List<HpoDisease> diseases = new ArrayList<>();
        Path annotPath = dataResolver.phenotypeAnnotations();
        Ontology hpo = MaxodiffBuilder.loadOntology(dataResolver.hpoJson());
        HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.defaultOptions();
        HpoDiseases hpoDiseases = MaxodiffBuilder.loadHpoDiseases(annotPath, hpo, options);
        diseaseIds.forEach(id -> hpoDiseases.diseaseById(id).ifPresent(diseases::add));
        return diseases;
    }

    public Map<SimpleTerm, Set<SimpleTerm>> makeHpoToMaxoTermMap(Map<SimpleTerm, Set<SimpleTerm>> fullHpoToMaxoTermMap,
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

    public Map<TermId, Set<TermId>> makeMaxoToHpoTermIdMap(Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap) {
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : hpoToMaxoTermMap.entrySet()) {
            TermId hpoId = entry.getKey().tid();
            Set<SimpleTerm> maxoTerms = entry.getValue();
            for (SimpleTerm maxoTerm : maxoTerms) {
                TermId maxoId = maxoTerm.tid();
                if (!maxoToHpoTermIdMap.containsKey(maxoId)) {
                    maxoToHpoTermIdMap.put(maxoId, new HashSet<>(Collections.singleton(hpoId)));
                } else {
                    Set<TermId> hpoIds = maxoToHpoTermIdMap.get(maxoId);
                    hpoIds.add(hpoId);
                    maxoToHpoTermIdMap.replace(maxoId, hpoIds);
                }
            }
        }
        return maxoToHpoTermIdMap;
    }

    public List<List<TermId>> getHpoTermCombos(List<TermId> hpoTermIds) {

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

    public Map<TermId, Double> makeMaxoScoreMap(Map<TermId, Set<TermId>> maxoToHpoTermIdMap, List<HpoDisease> diseases,
                                                AnalysisResults results, double weight) {
        Map<TermId, Double> maxoScoreMap = new HashMap<>();
        for (TermId maxoTermId : maxoToHpoTermIdMap.keySet()) {
            maxoScoreMap.put(maxoTermId, 0.0);
            // Collect HPO terms that can be ascertained by MAxO term
            List<TermId> hpoTermIds = maxoToHpoTermIdMap.get(maxoTermId).stream().toList();
            // Calculate differential diagnosis score, S, for HPO term combos
            List<List<TermId>> hpoCombos = getHpoTermCombos(hpoTermIds);
            List<Double> finalScores = new ArrayList<>();
            for (List<TermId> hpoCombo : hpoCombos) {
                // Get list of disease OMIM Ids associated with HPO term combo
                Set<TermId> omimIds = new HashSet<>();
                for (HpoDisease disease : diseases) {
                    List<TermId> annototationHpoIds = disease.annotationTermIdList();
                    for (TermId hpoId : hpoCombo) {
                        if (annototationHpoIds.contains(hpoId)) {
                            omimIds.add(disease.id());
                        }
                    }
                }
                // Calculate S using associated disease OMIM Ids
                double comboFinalScore = finalScore(results, omimIds.stream().toList(), weight);
                finalScores.add(comboFinalScore);
            }
            // Add max mean final score to map
            double maxoFinalScore = finalScores.stream().mapToDouble(s -> s).average().getAsDouble();
            if (maxoFinalScore > maxoScoreMap.get(maxoTermId)) {
                maxoScoreMap.replace(maxoTermId, maxoFinalScore);
            }
        }
        return maxoScoreMap;
    }

    public String getMaxoTermLabel(Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap, TermId maxScoreMaxoTermId) {
        String maxScoreTermLabel = new String();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> hpoToMaxoEntry : hpoToMaxoTermMap.entrySet()) {
            Set<SimpleTerm> maxoTerms = hpoToMaxoEntry.getValue();
            for (SimpleTerm maxoTerm : maxoTerms) {
                if (maxoTerm.tid().equals(maxScoreMaxoTermId)) {
                    maxScoreTermLabel = maxoTerm.label();
                    break;
                }
            }
        }
        return maxScoreTermLabel;
    }

}
