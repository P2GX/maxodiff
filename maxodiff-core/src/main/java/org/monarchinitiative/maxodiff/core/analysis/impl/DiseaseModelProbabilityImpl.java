package org.monarchinitiative.maxodiff.core.analysis.impl;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

/**
 * This class and its three subclasses are responsible for calculating the
 * probability of a disease according to its rank in a differential diagnosis (or its score).
 */
public sealed class DiseaseModelProbabilityImpl
        permits DiseaseRankedProbabilityImpl,
                DiseaseSoftmaxProbabilityImpl,
                DiseaseExponentialDecayProbabilityImpl {

      /** A list of differential diagnoses **/
    protected final List<DifferentialDiagnosis> differentialDiagnoses;

    public DiseaseModelProbabilityImpl(List<DifferentialDiagnosis> differentialDiagnoses) {
        this.differentialDiagnoses = differentialDiagnoses;
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
}
