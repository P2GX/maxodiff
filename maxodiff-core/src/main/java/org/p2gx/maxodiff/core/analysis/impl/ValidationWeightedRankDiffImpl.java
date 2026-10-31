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
 * <p>
 * For every disease in the initial ranking that also appears in the new ranking, the change in rank is
 * {@code |newRank - initialRank| / initialRank}, so a move near the top of the list counts more than the same
 * move lower down. The score is the sum of these changes. Diseases that appear in only one of the two rankings
 * do not contribute.
 */
public final class ValidationWeightedRankDiffImpl implements ValidationModel {

    private final double score;

    public ValidationWeightedRankDiffImpl(List<DifferentialDiagnosis> initialDifferentialDiagnoses,
                                          List<DifferentialDiagnosis> maxoDifferentialDiagnoses) {
        Map<TermId, Integer> newRanks = new HashMap<>();
        for (int i=0; i<maxoDifferentialDiagnoses.size();++i) {
            newRanks.putIfAbsent(maxoDifferentialDiagnoses.get(i).diseaseId(), i+1);
        }
        double sum = 0.0;
        for (int i=0; i < initialDifferentialDiagnoses.size(); ++i) {
            Integer newRank = newRanks.get(initialDifferentialDiagnoses.get(i).diseaseId());
            if (newRank != null) {
                int initialRank = i+1;
                sum += Math.abs(newRank - initialRank) / (double) initialRank;
            }
        }
        this.score = sum;
    }

    public double validationScore() {
        return score;
    }

}
