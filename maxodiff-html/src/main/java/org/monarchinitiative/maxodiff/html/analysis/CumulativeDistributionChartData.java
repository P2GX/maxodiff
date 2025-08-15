package org.monarchinitiative.maxodiff.html.analysis;

import org.apache.commons.math3.random.EmpiricalDistribution;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermScore;

import java.util.ArrayList;
import java.util.List;

public class CumulativeDistributionChartData {

    // TODO - should we delete this class?
    private CumulativeDistributionChartData(){}

    public record CumulativeDistributionRecord(String maxoId, Double score, Double probability) {}

    public static List<List<CumulativeDistributionRecord>> makeDistRecordList(List<List<MaxoTermScore>> allScoreRecords) {
        List<List<CumulativeDistributionRecord>> cumulativeDistributionRecordsList = new ArrayList<>();
        List<List<Double>> cumulativeDistributionList = new ArrayList<>();
        for (int i=0; i<allScoreRecords.size(); i++) {
            double[] distScores = new double[allScoreRecords.get(i).size()];
            List<CumulativeDistributionRecord> cumulativeDistributionRecords = new ArrayList<>();
            for (int j=0; j<distScores.length; j++) {
                distScores[j] = allScoreRecords.get(i).get(j).score();
            }
            cumulativeDistributionList.add(getScoreCumulativeDistribution(distScores));
            for (int j=0; j<distScores.length; j++) {
                String maxoId = allScoreRecords.get(i).get(j).maxoId();
                Double score = allScoreRecords.get(i).get(j).score();
                Double probability = cumulativeDistributionList.get(i).get(j);
                CumulativeDistributionRecord cumulativeDistributionRecord = new CumulativeDistributionRecord(maxoId, score, probability);
                cumulativeDistributionRecords.add(cumulativeDistributionRecord);
            }
            cumulativeDistributionRecordsList.add(cumulativeDistributionRecords);
        }
        return cumulativeDistributionRecordsList;
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
