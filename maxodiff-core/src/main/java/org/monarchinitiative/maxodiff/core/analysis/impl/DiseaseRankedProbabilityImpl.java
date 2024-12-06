package org.monarchinitiative.maxodiff.core.analysis.impl;

import org.monarchinitiative.maxodiff.core.analysis.DiseaseModelProbability;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

public class DiseaseRankedProbabilityImpl extends DiseaseModelProbabilityImpl implements DiseaseModelProbability {

    public DiseaseRankedProbabilityImpl(List<DifferentialDiagnosis> differentialDiagnoses) {
        super(differentialDiagnoses);
    }

    public double probability(TermId targetDiseaseId) {
        double targetDiagnosisScore = getTargetDiseaseDiagnosis(targetDiseaseId).score();

        double ddScoreSum = differentialDiagnoses().stream()
                .mapToDouble(DifferentialDiagnosis::score)
                .sum();

        return targetDiagnosisScore / ddScoreSum;
    }

}
