package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.model.SamplePhenopacket;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;

public class RankMaxo {

    private final Map<TermId, Set<TermId>> maxoToHpoTermIdMap;
    private final MaxoHpoTermProbabilities maxoHpoTermProbabilities;
    private final DifferentialDiagnosisEngine engine;

    public RankMaxo(Map<TermId, Set<TermId>> maxoToHpoTermIdMap, MaxoHpoTermProbabilities maxoHpoTermProbabilities, DifferentialDiagnosisEngine engine) {
        this.maxoToHpoTermIdMap = maxoToHpoTermIdMap;
        this.maxoHpoTermProbabilities = maxoHpoTermProbabilities;
        this.engine = engine;
    }

    /**
     *
     * @param ppkt Input phenopacket with present and excluded HPO terms.
     * @param nRepetitions number of times to calculate scores for each MAxO term.
     * @return Map of MAxO scores sorted in descending order by score
     */
    public Map<TermId, Double> rankMaxoTerms(Sample ppkt, int nRepetitions) {
        Map<TermId, Double> maxoScores = new HashMap<>();
        CandidateDiseaseScores candidateDiseaseScores = new CandidateDiseaseScores(maxoHpoTermProbabilities);
        for (TermId maxoId : maxoToHpoTermIdMap.keySet()) {
            List<Double> scores = new ArrayList<>();
            for (int i = 0; i < nRepetitions; i++) {
                double weight = 0.5;
                List<DifferentialDiagnosis> differentialDiagnoses = candidateDiseaseScores.getScoresForMaxoTerm(ppkt, maxoId, engine);
                List<Double> ddScores = differentialDiagnoses.stream().map(DifferentialDiagnosis::score).toList();
                double scoreSum = differentialDiagnoses.stream().mapToDouble(DifferentialDiagnosis::score).sum();
                double relativeDiseaseDiffSum = calculateRelDiseaseDiffEntropySum(ddScores);
                double finalScore = weight * scoreSum + (1 - weight) * relativeDiseaseDiffSum;
                scores.add(finalScore);
            }
            OptionalDouble meanScoreOptional = scores.stream().mapToDouble(s -> s).average();
            double meanScore = 0.0;
            if (meanScoreOptional.isPresent()) {
                meanScore = meanScoreOptional.getAsDouble();
            }
            maxoScores.put(maxoId, meanScore);
        }

        return maxoScores.entrySet().stream().sorted(Comparator.<Map.Entry<TermId, Double>>comparingDouble(Map.Entry::getValue)
                .reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a,b)->b, HashMap::new));
    }

    private static double calculateRelDiseaseDiffEntropySum(List<Double> differentialDiagnosisScores) {
        double sum = 0.0;
        for(Double score : differentialDiagnosisScores) {
            //TODO: double-check log base. Default is base e.
            sum += Math.log(score)*score;
        }
        return -sum;
    }

}
