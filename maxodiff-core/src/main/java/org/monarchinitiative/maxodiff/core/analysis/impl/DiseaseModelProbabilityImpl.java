package org.monarchinitiative.maxodiff.core.analysis.impl;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

public class DiseaseModelProbabilityImpl {

    private static final DiseaseModelProbabilityImpl EMPTY = new DiseaseModelProbabilityImpl(List.of());

    public static DiseaseModelProbabilityImpl empty() {
        return EMPTY;
    }

    /** A list of differential diagnoses **/
    private final List<DifferentialDiagnosis> differentialDiagnoses;

    public DiseaseModelProbabilityImpl(List<DifferentialDiagnosis> differentialDiagnoses) {
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

    public List<DifferentialDiagnosis> differentialDiagnoses() { return differentialDiagnoses; }


}
