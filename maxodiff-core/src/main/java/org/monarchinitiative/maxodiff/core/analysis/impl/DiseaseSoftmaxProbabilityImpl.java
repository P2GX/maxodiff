package org.monarchinitiative.maxodiff.core.analysis.impl;

import org.monarchinitiative.maxodiff.core.analysis.DiseaseModelProbability;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

public class DiseaseSoftmaxProbabilityImpl extends DiseaseModelProbabilityImpl  implements DiseaseModelProbability {

    public DiseaseSoftmaxProbabilityImpl(List<DifferentialDiagnosis> differentialDiagnoses) {
        super(differentialDiagnoses);
    }

    public double probability(TermId targetDiseaseId) {
        double targetDiagnosisScore = getTargetDiseaseDiagnosis(targetDiseaseId).score();

        double ddScoreSum = differentialDiagnoses().stream()
                .mapToDouble(dd -> Math.exp(dd.score()))
                .sum();

        return Math.exp(targetDiagnosisScore) / ddScoreSum;
    }

}
