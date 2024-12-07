package org.monarchinitiative.maxodiff.core.analysis.impl;

import org.monarchinitiative.maxodiff.core.analysis.DiseaseModelProbability;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

/**
 * The  softmax function is a normalized exponential function that converts scores into probabilities.
 */
public final class DiseaseSoftmaxProbabilityImpl extends DiseaseModelProbabilityImpl  implements DiseaseModelProbability {

    private final double ddScoreSum;

    public DiseaseSoftmaxProbabilityImpl(List<DifferentialDiagnosis> differentialDiagnoses) {
        super(differentialDiagnoses);
        ddScoreSum = differentialDiagnoses.stream()
                .mapToDouble(dd -> Math.exp(dd.score()))
                .sum();
    }

    public double probability(TermId targetDiseaseId) {
        double targetDiagnosisScore = getTargetDiseaseDiagnosis(targetDiseaseId).score();
        return Math.exp(targetDiagnosisScore) / ddScoreSum;
    }

}
