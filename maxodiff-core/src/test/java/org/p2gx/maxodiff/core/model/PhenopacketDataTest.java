package org.p2gx.maxodiff.core.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PhenopacketDataTest {

    private static final SimpleTerm OBSERVED = new SimpleTerm(TermId.of("HP:0000001"), "observed");
    private static final SimpleTerm EXCLUDED = new SimpleTerm(TermId.of("HP:0000002"), "excluded");
    private static final SimpleTerm NEW_A = new SimpleTerm(TermId.of("HP:0000003"), "new a");
    private static final SimpleTerm NEW_B = new SimpleTerm(TermId.of("HP:0000004"), "new b");
    private static final TermId DISEASE = TermId.of("OMIM:100001");
    private static final TermId PROCEDURE = TermId.of("MAXO:0000001");

    private PhenopacketData original;

    @BeforeEach
    void setUp() {
        original = new PhenopacketData("sample1", List.of(OBSERVED), List.of(EXCLUDED),
                List.of(DISEASE), List.of(PROCEDURE), false);
    }

    @Test
    void additionalTermsAreAppendedToTheObservedOnes() {
        PhenopacketData copy = original.withAdditionalObserved(List.of(NEW_A, NEW_B));
        assertEquals(List.of(OBSERVED, NEW_A, NEW_B), copy.observed());
    }

    @Test
    void everythingElseIsKept() {
        PhenopacketData copy = original.withAdditionalObserved(List.of(NEW_A));
        assertEquals("sample1", copy.sampleId());
        assertEquals(List.of(EXCLUDED), copy.excluded());
        assertEquals(List.of(DISEASE), copy.diseaseIds());
        assertEquals(List.of(PROCEDURE), copy.maxoProcedureIds());
    }

    @Test
    void aTermThatIsAlreadyObservedIsNotRepeated() {
        PhenopacketData copy = original.withAdditionalObserved(Set.of(OBSERVED));
        assertEquals(List.of(OBSERVED), copy.observed());
    }

    @Test
    void addingNothingGivesTheSameObservedTerms() {
        assertEquals(original.observed(), original.withAdditionalObserved(List.of()).observed());
    }

    @Test
    void theOriginalIsNotChanged() {
        original.withAdditionalObserved(List.of(NEW_A));
        assertEquals(List.of(OBSERVED), original.observed());
    }
}