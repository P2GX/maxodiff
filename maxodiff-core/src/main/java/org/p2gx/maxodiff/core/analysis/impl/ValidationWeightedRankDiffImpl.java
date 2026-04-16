package org.p2gx.maxodiff.core.analysis.impl;

import org.p2gx.maxodiff.core.analysis.ValidationModel;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validate the effectiveness of the maxodiff algorithm using the weighted sum of differences of disease ranks before and after
 * applying the maxodiff algorithm.
 */
public final class ValidationWeightedRankDiffImpl implements ValidationModel {

    private final Map<TermId, Double> rankChanges = new HashMap<>();

    public ValidationWeightedRankDiffImpl(List<DifferentialDiagnosis> initialDifferentialDiagnoses,
                                          List<DifferentialDiagnosis> maxoDifferentialDiagnoses) {

        for (DifferentialDiagnosis initialDiagnosis : initialDifferentialDiagnoses) {
            TermId termId = initialDiagnosis.diseaseId();
            double initialRank = initialDifferentialDiagnoses.indexOf(initialDiagnosis) + 1;
            List<DifferentialDiagnosis> maxoDiagnoses = maxoDifferentialDiagnoses.stream()
                    .filter(dd -> dd.diseaseId().equals(termId)).toList();
            if (!maxoDiagnoses.isEmpty()) {
                DifferentialDiagnosis maxoDiagnosis = maxoDiagnoses.getFirst();
                double maxoRank = maxoDifferentialDiagnoses.indexOf(maxoDiagnosis) + 1;
                double rankChange = Math.abs(maxoRank - initialRank) / initialRank;
                rankChanges.put(termId, rankChange);
            }
        }

    }

    public double validationScore() {
        return rankChanges.values().stream().mapToDouble(diff -> diff).sum();
    }

}
