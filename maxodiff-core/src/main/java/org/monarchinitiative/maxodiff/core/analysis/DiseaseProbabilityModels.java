package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

public class DiseaseProbabilityModels {

    private final List<DifferentialDiagnosis> differentialDiagnoses;

    public DiseaseProbabilityModels(List<DifferentialDiagnosis> differentialDiagnoses) {
        this.differentialDiagnoses = differentialDiagnoses;
    }

    public DifferentialDiagnosis getTargetDiseaseDiagnosis(TermId targetDiseaseId) {
        List<DifferentialDiagnosis> targetDiagnosisList = differentialDiagnoses.stream()
                .filter(dd -> dd.diseaseId().equals(targetDiseaseId))
                .toList();

        if (targetDiagnosisList.isEmpty()) {
            throw new PhenolRuntimeException("Could not find disease id " + targetDiseaseId.getValue() + " in differential diagnoses");
        }

        return targetDiagnosisList.getFirst();
    }

    public double ranked(TermId targetDiseaseId) {
        double targetDiagnosisScore = getTargetDiseaseDiagnosis(targetDiseaseId).score();

        double ddScoreSum = differentialDiagnoses.stream()
                .mapToDouble(DifferentialDiagnosis::score)
                .sum();

        return targetDiagnosisScore / ddScoreSum;
    }

    public double softmax(TermId targetDiseaseId) {
        double targetDiagnosisScore = getTargetDiseaseDiagnosis(targetDiseaseId).score();

        double ddScoreSum = differentialDiagnoses.stream()
                .mapToDouble(dd -> Math.exp(dd.score()))
                .sum();

        return Math.exp(targetDiagnosisScore) / ddScoreSum;
    }

    public double exponentialDecay(TermId targetDiseaseId, double lambda) {
        double targetDiagnosisRank = differentialDiagnoses.indexOf(getTargetDiseaseDiagnosis(targetDiseaseId)) + 1;

        double ddRankSum = differentialDiagnoses.stream()
                .mapToDouble(dd -> Math.exp(-lambda * (differentialDiagnoses.indexOf(dd) + 1)))
                .sum();

        return Math.exp(-lambda * targetDiagnosisRank) / ddRankSum;
    }
}
