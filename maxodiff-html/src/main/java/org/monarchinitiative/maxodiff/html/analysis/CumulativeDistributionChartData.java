package org.monarchinitiative.maxodiff.html.analysis;

import org.monarchinitiative.maxodiff.core.analysis.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermMap;

import java.util.ArrayList;
import java.util.List;

public class CumulativeDistributionChartData {

    public record CumulativeDistributionRecord(String maxoId, String maxoLabel, Double score, Double probability) {}

    public static List<List<CumulativeDistributionRecord>> makeDistRecordList(List<List<MaxoTermMap.MaxoTermScore>> allScoreRecords) {
        List<List<CumulativeDistributionRecord>> cumulativeDistributionRecordsList = new ArrayList<>();
        List<List<Double>> cumulativeDistributionList = new ArrayList<>();
        for (int i=0; i<allScoreRecords.size(); i++) {
            double[] distScores = new double[allScoreRecords.get(i).size()];
            List<CumulativeDistributionRecord> cumulativeDistributionRecords = new ArrayList<>();
            for (int j=0; j<distScores.length; j++) {
                distScores[j] = allScoreRecords.get(i).get(j).score();
            }
            cumulativeDistributionList.add(DifferentialDiagnosis.getScoreCumulativeDistribution(distScores));
            for (int j=0; j<distScores.length; j++) {
                String maxoId = allScoreRecords.get(i).get(j).maxoId();
                String maxoLabel = allScoreRecords.get(i).get(j).maxoLabel();
                Double score = allScoreRecords.get(i).get(j).score();
                Double probability = cumulativeDistributionList.get(i).get(j);
                CumulativeDistributionRecord cumulativeDistributionRecord = new CumulativeDistributionRecord(maxoId, maxoLabel, score, probability);
                cumulativeDistributionRecords.add(cumulativeDistributionRecord);
            }
            cumulativeDistributionRecordsList.add(cumulativeDistributionRecords);
        }
        return cumulativeDistributionRecordsList;
    }

}
