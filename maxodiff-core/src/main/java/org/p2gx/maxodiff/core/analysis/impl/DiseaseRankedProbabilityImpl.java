package org.p2gx.maxodiff.core.analysis.impl;

import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.p2gx.maxodiff.core.model.DiseaseModelProbability;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

/**
 * The probability of an item  being the correct diagnosis is proportional to its score.
 */
public final class DiseaseRankedProbabilityImpl implements DiseaseModelProbability {

    protected final List<DifferentialDiagnosis> differentialDiagnoses;
    private final double ddScoreSum;

    public DiseaseRankedProbabilityImpl(List<DifferentialDiagnosis> differentialDiagnoses) {
        this.differentialDiagnoses = differentialDiagnoses;
        this.ddScoreSum = differentialDiagnoses.stream()
                .mapToDouble(DifferentialDiagnosis::score)
                .sum();
    }

    protected DifferentialDiagnosis getTargetDiseaseDiagnosis(TermId targetDiseaseId) {
        List<DifferentialDiagnosis> targetDiagnosisList = differentialDiagnoses.stream()
                .filter(dd -> dd.diseaseId().equals(targetDiseaseId))
                .toList();

        if (targetDiagnosisList.isEmpty()) {
            throw new PhenolRuntimeException("Could not find disease id " + targetDiseaseId.getValue() + " in differential diagnoses");
        }

        return targetDiagnosisList.getFirst();
    }

    public double probability(TermId targetDiseaseId) {
        double targetDiagnosisScore = getTargetDiseaseDiagnosis(targetDiseaseId).score();
        return targetDiagnosisScore / ddScoreSum;
    }

}
