package org.monarchinitiative.maxodiff.core.analysis.impl;

import org.monarchinitiative.maxodiff.core.analysis.DiseaseModelProbability;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

/**
 * The probability of an item  being the correct diagnosis is proportional to its score.
 */
public final class DiseaseRankedProbabilityImpl extends DiseaseModelProbabilityImpl implements DiseaseModelProbability {

    private final double ddScoreSum;

    public DiseaseRankedProbabilityImpl(List<DifferentialDiagnosis> differentialDiagnoses) {
        super(differentialDiagnoses);
        this.ddScoreSum = differentialDiagnoses.stream()
                .mapToDouble(DifferentialDiagnosis::score)
                .sum();
    }

    public double probability(TermId targetDiseaseId) {
        double targetDiagnosisScore = getTargetDiseaseDiagnosis(targetDiseaseId).score();
        return targetDiagnosisScore / ddScoreSum;
    }

}
