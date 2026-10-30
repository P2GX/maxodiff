package org.p2gx.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import static org.junit.jupiter.api.Assertions.*;

class HpoWeightTest {

    private static final TermId A = TermId.of("HP:0000001");

    @Test
    void acceptsPositiveFiniteWeight() {
        assertEquals(0.5, new HpoWeight(A, 0.5).weight());
    }

    @Test
    void rejectsZeroNegativeNanAndInfinity() {
        assertThrows(IllegalArgumentException.class, () -> new HpoWeight(A, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new HpoWeight(A, -1.0));
        assertThrows(IllegalArgumentException.class, () -> new HpoWeight(A, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new HpoWeight(A, Double.POSITIVE_INFINITY));
    }

    @Test
    void rejectsNullTerm() {
        assertThrows(NullPointerException.class, () -> new HpoWeight(null, 1.0));
    }
}