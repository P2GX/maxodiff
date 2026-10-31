package org.p2gx.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;

import java.util.ArrayList;
import java.util.List;

/** Summarises how the rank of one disease changed between the initial differential diagnosis and the simulations. */
final class DiseaseRankSummary {

    private DiseaseRankSummary() {
    }

    /**
     * @return the 1-based position of the disease in the list, or {@code 0} if it is not in the list
     */
    static int rankOf(List<DifferentialDiagnosis> diagnoses, TermId diseaseId) {
        for (int i = 0; i < diagnoses.size(); i++) {
            if (diagnoses.get(i).diseaseId().equals(diseaseId)) {
                return i + 1; // 1-based rank
            }
        }
        return 0;
    }

    /**
     * @param disease        the disease, with its label
     * @param initialDiagnoses the original differential diagnosis
     * @param newRankings    the differential diagnosis from each simulated repetition
     * @return the initial rank of the disease and its average rank over the repetitions in which it was ranked
     */
    static RankedOmimTerm summarize(SimpleTerm disease,
                                    List<DifferentialDiagnosis> initialDiagnoses,
                                    List<List<DifferentialDiagnosis>> newRankings) {
        int initialRank = rankOf(initialDiagnoses, disease.tid());

        List<Integer> newRanks = new ArrayList<>();
        for (List<DifferentialDiagnosis> newList : newRankings) {
            int newRank = rankOf(newList, disease.tid());
            if (newRank > 0) {
                newRanks.add(newRank);
            }
        }

        // Assumption: every disease is ranked in every repetition, as has been the case in practice. If a disease
        // were never ranked, this average would be 0, which would look like a rank better than first place.
        float averageRank = (float) newRanks.stream().mapToInt(Integer::intValue).average().orElse(0);
        return new RankedOmimTerm(disease, initialRank, averageRank);
    }
}