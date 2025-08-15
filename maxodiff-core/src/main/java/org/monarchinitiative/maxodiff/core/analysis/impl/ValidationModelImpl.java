package org.monarchinitiative.maxodiff.core.analysis.impl;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;

/**
 * This class and its three subclasses are responsible for validating the effectiveness
 * of the maxodiff algorithm by comparing a disease rank in a differential diagnosis (or its score),
 * before and after the maxodiff procedure.
 */
public sealed class ValidationModelImpl
        permits ValidationRankDiffImpl,
                ValidationWeightedRankDiffImpl,
                ValidationScoreDiffImpl {

    /** A list of the initial differential diagnoses **/
    protected final List<DifferentialDiagnosis> initialDifferentialDiagnoses;
    /** A map of MAxO term ids : differential diagnoses after running the maxodiff analysis **/
    protected final List<DifferentialDiagnosis> maxoDifferentialDiagnoses;

    public ValidationModelImpl(List<DifferentialDiagnosis> initialDifferentialDiagnoses,
                               List<DifferentialDiagnosis> maxoDifferentialDiagnoses) {
        this.initialDifferentialDiagnoses = initialDifferentialDiagnoses;
        this.maxoDifferentialDiagnoses = maxoDifferentialDiagnoses;
    }

    protected DifferentialDiagnosis getTargetDiseaseDiagnosis(TermId targetDiseaseId, List<DifferentialDiagnosis> differentialDiagnoses) {
        List<DifferentialDiagnosis> targetDiagnosisList = differentialDiagnoses.stream()
                .filter(dd -> dd.diseaseId().equals(targetDiseaseId))
                .toList();

        if (targetDiagnosisList.isEmpty()) {
            throw new PhenolRuntimeException("Could not find disease id " + targetDiseaseId.getValue() + " in differential diagnoses");
        }

        return targetDiagnosisList.getFirst();
    }
}
