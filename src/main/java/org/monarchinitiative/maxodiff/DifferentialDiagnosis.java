package org.monarchinitiative.maxodiff;

import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Comparator;
import java.util.List;

public class DifferentialDiagnosis {

    public double posttestProbabilitySum(AnalysisResults results, List<TermId> diseaseIds) {
        double sum = 0.0;
        for (TermId id : diseaseIds) {
            var result = results.resultByDiseaseId(id);
            if (result.isPresent())
                sum += result.get().posttestProbability();
        }
        return sum;
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


}
