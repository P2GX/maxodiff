package org.monarchinitiative.maxodiff.core.analysis;

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

    //Functions using results from a separate LIRICAL output file
    public static double posttestProbabilitySum(List<LiricalResultsFileRecord> liricalOutputRecords,
                                                List<TermId> diseaseIds) {
        double sum = 0.0;
        for (TermId id : diseaseIds) {
            var liricalRecord = liricalOutputRecords.stream().filter(r -> r.omimId().equals(id)).findFirst();
            if (liricalRecord.isPresent())
                sum += liricalRecord.get().posttestProbability();
        }
        return sum / 100;
    }

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
            List<Double> diffLRList = diffResultsList.stream().map(LiricalResultsFileRecord::likelihoodRatio).toList();
            double targetLR = targetResult.likelihoodRatio();
            for (double lr : diffLRList)
                sum += targetLR / lr;
        }
        return sum;
    }

    public static double scoreSum(List<LiricalResultsFileRecord> liricalOutputRecords,
                                  List<TermId> diseaseIds) {
        double sum = 0.0;
        List<TermId> subIds = diseaseIds.subList(0, diseaseIds.size());
        for (TermId diseaseId : subIds)
            sum += relativeDiseaseDiff(liricalOutputRecords, diseaseIds, diseaseId);
        return sum;
    }

    public static double finalScore(List<LiricalResultsFileRecord> liricalOutputRecords,
                                    List<TermId> diseaseIds, double weight) {
        double p = posttestProbabilitySum(liricalOutputRecords, diseaseIds);
        double q = scoreSum(liricalOutputRecords, diseaseIds);
        return weight*p + (1-weight)*q;
    }

    public static List<HpoDisease> makeDiseaseList(MaxodiffDataResolver dataResolver, List<TermId> diseaseIds) throws MaxodiffDataException {
        List<HpoDisease> diseases = new ArrayList<>();
        Path annotPath = dataResolver.phenotypeAnnotations();
        Ontology hpo = MaxodiffBuilder.loadOntology(dataResolver.hpoJson());
        HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.defaultOptions();
        HpoDiseases hpoDiseases = MaxodiffBuilder.loadHpoDiseases(annotPath, hpo, options);
        diseaseIds.forEach(id -> hpoDiseases.diseaseById(id).ifPresent(diseases::add));
        return diseases;
    }

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

    public static Map<SimpleTerm, Double> makeMaxoScoreMap(Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoTermMap, List<HpoDisease> diseases,
                                                       List<LiricalResultsFileRecord> liricalOutputRecords, double weight) {
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
                double comboFinalScore = finalScore(liricalOutputRecords, omimIds.stream().toList(), weight);
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

}
