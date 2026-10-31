package org.p2gx.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        // One pass over the (long) list, instead of one pass per selected term
        Map<TermId, List<HpoFrequency>> byHpoTerm = frequencies.stream()
                .collect(Collectors.groupingBy(HpoFrequency::hpoId));

        List<HpoFrequency> frequencyRecords = new ArrayList<>();
        for (SimpleTerm hpoTerm : hpoTerms) {
            for (HpoFrequency hpoFrequency : byHpoTerm.getOrDefault(hpoTerm.tid(), List.of())) {
                if (diseaseIds.contains(hpoFrequency.diseaseId())) {
                    frequencyRecords.add(hpoFrequency);
                }
            }
        }
        return frequencyRecords;
    }
}