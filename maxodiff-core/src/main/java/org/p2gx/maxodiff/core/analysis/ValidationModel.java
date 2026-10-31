package org.p2gx.maxodiff.core.analysis;


import org.p2gx.maxodiff.core.analysis.impl.ValidationWeightedRankDiffImpl;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;


import java.util.List;

public interface ValidationModel {

    double validationScore();


    static ValidationModel weightedRankDiff(List<DifferentialDiagnosis> initialDifferentialDiagnoses,
                                            List<DifferentialDiagnosis> maxoDifferentialDiagnoses) {
        return new ValidationWeightedRankDiffImpl(initialDifferentialDiagnoses, maxoDifferentialDiagnoses);
    }

}
