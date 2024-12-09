package org.monarchinitiative.maxodiff.core.analysis.impl;

import org.monarchinitiative.maxodiff.core.analysis.DiseaseModelProbability;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

/**
 * Assign probabilities using an exponential decay function based on the rank.
 */
public final class DiseaseExponentialDecayProbabilityImpl extends DiseaseModelProbabilityImpl  implements DiseaseModelProbability {

    private final double lambda;
    /** Sum of the ranks with exponential decay. */
    private final double ddRankSum;

    public DiseaseExponentialDecayProbabilityImpl(List<DifferentialDiagnosis> differentialDiagnoses, double lambda) {
        super(differentialDiagnoses);
        this.lambda = lambda;
        this.ddRankSum = differentialDiagnoses.stream()
                .mapToDouble(dd -> Math.exp(-lambda * (differentialDiagnoses.indexOf(dd) + 1)))
                .sum();
    }

    public double probability(TermId targetDiseaseId) {
        double targetDiagnosisRank = differentialDiagnoses.indexOf(getTargetDiseaseDiagnosis(targetDiseaseId)) + 1;
        return Math.exp(-lambda * targetDiagnosisRank) / ddRankSum;
    }

}
