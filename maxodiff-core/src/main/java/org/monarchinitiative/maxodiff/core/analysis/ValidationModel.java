package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.analysis.impl.*;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;

import java.util.List;

public interface ValidationModel {

    double validationScore();

    static ValidationModel rankDiff(List<DifferentialDiagnosis> initialDifferentialDiagnoses,
                                    List<DifferentialDiagnosis> maxoDifferentialDiagnoses) {
        return new ValidationRankDiffImpl(initialDifferentialDiagnoses, maxoDifferentialDiagnoses);
    }

    static ValidationModel weightedRankDiff(List<DifferentialDiagnosis> initialDifferentialDiagnoses,
                                            List<DifferentialDiagnosis> maxoDifferentialDiagnoses) {
        return new ValidationWeightedRankDiffImpl(initialDifferentialDiagnoses, maxoDifferentialDiagnoses);
    }

    static ValidationModel scoreDiff(List<DifferentialDiagnosis> initialDifferentialDiagnoses,
                                     List<DifferentialDiagnosis> maxoDifferentialDiagnoses) {
        return new ValidationScoreDiffImpl(initialDifferentialDiagnoses, maxoDifferentialDiagnoses);
    }

}
