package org.monarchinitiative.maxodiff.core.analysis.impl;

import org.monarchinitiative.maxodiff.core.analysis.ValidationModel;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validate the effectiveness of the maxodiff algorithm using the sum of differences of disease scores before and after
 * applying the maxodiff algorithm.
 */
public final class ValidationScoreDiffImpl extends ValidationModelImpl  implements ValidationModel {

    Map<TermId, Double> scoreChanges = new HashMap<>();

    public ValidationScoreDiffImpl(List<DifferentialDiagnosis> initialDifferentialDiagnoses,
                                   List<DifferentialDiagnosis> maxoDifferentialDiagnoses) {
        super(initialDifferentialDiagnoses, maxoDifferentialDiagnoses);

        for (DifferentialDiagnosis initialDiagnosis : initialDifferentialDiagnoses) {
            TermId termId = initialDiagnosis.diseaseId();
            double initialScore = initialDiagnosis.score();
            List<DifferentialDiagnosis> maxoDiagnoses = maxoDifferentialDiagnoses.stream()
                    .filter(dd -> dd.diseaseId().equals(termId)).toList();
            if (!maxoDiagnoses.isEmpty()) {
                DifferentialDiagnosis maxoDiagnosis = maxoDiagnoses.getFirst();
                double maxoScore = maxoDiagnosis.score();
                double scoreChange = Math.abs(maxoScore - initialScore);
                scoreChanges.put(termId, scoreChange);
            }
        }

    }

    public double validationScore() {
        return scoreChanges.values().stream().mapToDouble(diff -> diff).sum();
    }

}
