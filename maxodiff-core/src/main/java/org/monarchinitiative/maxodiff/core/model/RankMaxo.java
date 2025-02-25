package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.ValidationModel;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;

public class RankMaxo {

    private final Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap;
    private final Map<TermId, Set<TermId>> maxoToHpoTermIdMap;
    private final MaxoHpoTermProbabilities maxoHpoTermProbabilities;
    private final DifferentialDiagnosisEngine engine;
    double p;

    public RankMaxo(Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap,
                    Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                    MaxoHpoTermProbabilities maxoHpoTermProbabilities,
                    DifferentialDiagnosisEngine engine) {
        this.hpoToMaxoTermMap = hpoToMaxoTermMap;
        this.maxoToHpoTermIdMap = maxoToHpoTermIdMap;
        this.maxoHpoTermProbabilities = maxoHpoTermProbabilities;
        this.engine = engine;
    }

    /**
     *
     * @param ppkt Input phenopacket with present and excluded HPO terms.
     * @param nRepetitions number of times to calculate scores for each MAxO term.
     * @param diseaseIds Set of OMIM disease Ids to use for analysis.
     * @return Map of MAxO scores sorted in descending order by score
     */
    public Map<TermId, Double> rankMaxoTerms(Sample ppkt, int nRepetitions, Set<TermId> diseaseIds) {
        Map<TermId, Double> maxoScores = new HashMap<>();
        CandidateDiseaseScores candidateDiseaseScores = new CandidateDiseaseScores(maxoHpoTermProbabilities);
        p = 0;
        for (TermId maxoId : maxoToHpoTermIdMap.keySet()) {
            List<Double> scores = new ArrayList<>();
            List<DifferentialDiagnosis> initialDiagnoses = maxoHpoTermProbabilities.getInitialDiagnoses();
            for (int i = 0; i < nRepetitions; i++) {
                List<DifferentialDiagnosis> differentialDiagnoses = candidateDiseaseScores.getScoresForMaxoTerm(ppkt, maxoId, engine, diseaseIds, hpoToMaxoTermMap);
                double finalScore = ValidationModel.weightedRankDiff(initialDiagnoses, differentialDiagnoses).validationScore();
                scores.add(finalScore);
            }
            OptionalDouble meanScoreOptional = scores.stream().mapToDouble(s -> s).average();
            double meanScore = 0.0;
            if (meanScoreOptional.isPresent()) {
                meanScore = meanScoreOptional.getAsDouble();
            }
            maxoScores.put(maxoId, meanScore);
            p++;
            updateProgress();
        }

        return maxoScores.entrySet().stream()
                .sorted(Comparator.<Map.Entry<TermId, Double>>comparingDouble(Map.Entry::getValue)
                .reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a,b)->b, LinkedHashMap::new));
    }

    private static double calculateRelDiseaseDiffEntropySum(List<Double> differentialDiagnosisScores) {
        double sum = 0.0;
        final double EPSILON = 1e-10;
        for(Double score : differentialDiagnosisScores) {
            //TODO: double-check log base. Default is base e.
            sum += Math.abs(score) < EPSILON ? 0 : Math.log(score)*score;
        }
        return -sum;
    }

    public double updateProgress() {
        int nMaxoTermIds = maxoToHpoTermIdMap.keySet().size();
        return (p / nMaxoTermIds) * 100.;

    }

}
