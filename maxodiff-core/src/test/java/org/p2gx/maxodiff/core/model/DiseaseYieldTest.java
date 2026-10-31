package org.p2gx.maxodiff.core.model;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DiseaseYieldTest {

    private static final DifferentialDiagnosis DX =
            DifferentialDiagnosis.of(TermId.of("OMIM:100000"), 1.0, 1.0);

    private static final TermId HP_A = TermId.of("HP:0000001");
    private static final TermId HP_B = TermId.of("HP:0000002");
    private static final TermId HP_C = TermId.of("HP:0000003");
    private static final TermId HP_D = TermId.of("HP:0000004");

    @Test
    void weightIsReciprocalOfRank() {
        assertEquals(1.0, new DiseaseYield(DX, 1, Set.of()).weight());
        assertEquals(0.5, new DiseaseYield(DX, 2, Set.of()).weight());
        assertEquals(0.25, new DiseaseYield(DX, 4, Set.of()).weight());
    }

    @Test
    void ofKeepsOnlyTermsTheMaxoCanDetect() {
        Set<TermId> assessable = Set.of(HP_A, HP_B, HP_C);
        Set<TermId> maxoTerms = Set.of(HP_B, HP_C, HP_D);

        DiseaseYield y = DiseaseYield.of(DX, 1, assessable, maxoTerms);

        assertEquals(Set.of(HP_B, HP_C), y.revealedByMaxo());
    }

    @Test
    void countEqualsNumberOfRevealedTerms() {
        DiseaseYield y = DiseaseYield.of(DX, 3, Set.of(HP_A, HP_B, HP_C), Set.of(HP_A, HP_C));
        assertEquals(2, y.count());
    }

    @Test
    void noOverlapGivesEmptyYield() {
        DiseaseYield y = DiseaseYield.of(DX, 1, Set.of(HP_A), Set.of(HP_D));
        assertTrue(y.revealedByMaxo().isEmpty());
        assertEquals(0, y.count());
    }

    @Test
    void emptyAssessableGivesEmptyYield() {
        DiseaseYield y = DiseaseYield.of(DX, 1, Set.of(), Set.of(HP_A));
        assertEquals(0, y.count());
    }

    @Test
    void ofDoesNotMutateItsInputs() {
        Set<TermId> assessable = new HashSet<>(Set.of(HP_A, HP_B));
        Set<TermId> maxoTerms = new HashSet<>(Set.of(HP_B, HP_C));

        DiseaseYield.of(DX, 1, assessable, maxoTerms);

        assertEquals(Set.of(HP_A, HP_B), assessable);
        assertEquals(Set.of(HP_B, HP_C), maxoTerms);
    }

    @Test
    void revealedTermsAreDefensivelyCopied() {
        Set<TermId> mutable = new HashSet<>(Set.of(HP_A));
        DiseaseYield y = new DiseaseYield(DX, 1, mutable);

        mutable.add(HP_B); // later change by the caller must not leak in

        assertEquals(Set.of(HP_A), y.revealedByMaxo());
    }

    @Test
    void revealedTermsAreImmutable() {
        DiseaseYield y = new DiseaseYield(DX, 1, Set.of(HP_A));
        assertThrows(UnsupportedOperationException.class, () -> y.revealedByMaxo().add(HP_B));
    }

    @Test
    void rankBelowOneIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new DiseaseYield(DX, 0, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new DiseaseYield(DX, -1, Set.of()));
    }

    @Test
    void nullArgumentsAreRejected() {
        assertThrows(NullPointerException.class, () -> new DiseaseYield(null, 1, Set.of()));
        assertThrows(NullPointerException.class, () -> new DiseaseYield(DX, 1, null));
    }
}