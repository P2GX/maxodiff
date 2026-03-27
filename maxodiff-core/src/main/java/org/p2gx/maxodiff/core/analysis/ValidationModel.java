package org.p2gx.maxodiff.core.analysis;

import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;
import org.p2gx.maxodiff.core.analysis.impl.ValidationRankDiffImpl;
import org.p2gx.maxodiff.core.analysis.impl.ValidationScoreDiffImpl;
import org.p2gx.maxodiff.core.analysis.impl.ValidationWeightedRankDiffImpl;

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
