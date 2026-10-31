package org.p2gx.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HpoFrequencyFilterTest {

    private static final TermId D1 = TermId.of("OMIM:100001");
    private static final TermId D2 = TermId.of("OMIM:100002");
    private static final TermId D3 = TermId.of("OMIM:100003");
    private static final TermId H1 = TermId.of("HP:0000001");
    private static final TermId H2 = TermId.of("HP:0000002");
    private static final TermId H3 = TermId.of("HP:0000003");

    private static final HpoFrequency F1 = new HpoFrequency(D1, H1, 0.5f, 1.0f);
    private static final HpoFrequency F2 = new HpoFrequency(D2, H1, 0.6f, 1.0f);
    private static final HpoFrequency F3 = new HpoFrequency(D1, H2, 0.7f, 2.0f);
    private static final HpoFrequency F4 = new HpoFrequency(D3, H1, 0.8f, 1.0f);
    private static final HpoFrequency F5 = new HpoFrequency(D1, H3, 0.9f, 3.0f);
    private static final List<HpoFrequency> ALL = List.of(F1, F2, F3, F4, F5);

    private static Set<SimpleTerm> terms(TermId... ids) {
        Set<SimpleTerm> set = new LinkedHashSet<>();
        for (TermId id : ids) {
            set.add(new SimpleTerm(id, "label " + id.getValue()));
        }
        return set;
    }

    @Test
    void keepsOnlyRecordsForTheChosenTermAndDiseases() {
        // F4 is for H1 but disease D3 is not wanted
        assertEquals(List.of(F1, F2), HpoFrequencyFilter.select(Set.of(D1, D2), terms(H1), ALL));
    }

    @Test
    void resultIsGroupedByTermInTheOrderOfTheTerms() {
        // H2 first, then H1: F3, then F1, F2, F4 in their original order
        assertEquals(List.of(F3, F1, F2, F4), HpoFrequencyFilter.select(Set.of(D1, D2, D3), terms(H2, H1), ALL));
    }

    @Test
    void aTermWithNoRecordsContributesNothing() {
        assertTrue(HpoFrequencyFilter.select(Set.of(D1), terms(TermId.of("HP:0000999")), ALL).isEmpty());
    }

    @Test
    void termsAreMatchedByIdNotLabel() {
        Set<SimpleTerm> differentLabel = Set.of(new SimpleTerm(H1, "a different label"));
        assertEquals(List.of(F1), HpoFrequencyFilter.select(Set.of(D1), differentLabel, ALL));
    }

    @Test
    void emptyInputsGiveAnEmptyResult() {
        assertTrue(HpoFrequencyFilter.select(Set.of(D1), Set.of(), ALL).isEmpty());
        assertTrue(HpoFrequencyFilter.select(Set.of(), terms(H1), ALL).isEmpty());
        assertTrue(HpoFrequencyFilter.select(Set.of(D1), terms(H1), List.of()).isEmpty());
    }
}