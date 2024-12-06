package org.monarchinitiative.maxodiff.core.analysis.impl;

import org.monarchinitiative.maxodiff.core.analysis.DiseaseModelProbability;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

public class DiseaseExponentialDecayProbabilityImpl extends DiseaseModelProbabilityImpl  implements DiseaseModelProbability {

    private final double lambda;

    public DiseaseExponentialDecayProbabilityImpl(List<DifferentialDiagnosis> differentialDiagnoses, double lambda) {
        super(differentialDiagnoses);
        this.lambda = lambda;
    }

    public double probability(TermId targetDiseaseId) {
        double targetDiagnosisRank = differentialDiagnoses().indexOf(getTargetDiseaseDiagnosis(targetDiseaseId)) + 1;

        double ddRankSum = differentialDiagnoses().stream()
                .mapToDouble(dd -> Math.exp(-lambda * (differentialDiagnoses().indexOf(dd) + 1)))
                .sum();

        return Math.exp(-lambda * targetDiagnosisRank) / ddRankSum;
    }

}
