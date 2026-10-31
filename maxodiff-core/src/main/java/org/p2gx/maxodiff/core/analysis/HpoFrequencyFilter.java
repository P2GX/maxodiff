package org.p2gx.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Selects the HPO frequency records that are relevant to one MAxO result. */
final class HpoFrequencyFilter {

    private HpoFrequencyFilter() {
    }

    /**
     * @param diseaseIds  diseases of interest
     * @param hpoTerms    HPO terms of interest; the result is grouped by term, in this order
     * @param frequencies all known disease/HPO frequency records
     * @return the records for a term in {@code hpoTerms} and a disease in {@code diseaseIds}; for each term,
     *         in the order they have in {@code frequencies}
     */
    static List<HpoFrequency> select(Set<TermId> diseaseIds, Set<SimpleTerm> hpoTerms, List<HpoFrequency> frequencies) {
        List<HpoFrequency> frequencyRecords = new ArrayList<>();
        for (SimpleTerm hpoTerm : hpoTerms) {
            TermId hpoId = hpoTerm.tid();
            List<HpoFrequency> matching = frequencies.stream().filter(f -> f.hpoId().equals(hpoId)).toList();
            for (HpoFrequency hpoFrequency : matching) {
                for (TermId omimId : diseaseIds) {
                    if (hpoFrequency.diseaseId().equals(omimId)) {
                        frequencyRecords.add(hpoFrequency);
                    }
                }
            }
        }
        return frequencyRecords;
    }
}