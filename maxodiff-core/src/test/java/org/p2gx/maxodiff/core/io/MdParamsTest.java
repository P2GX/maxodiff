package org.p2gx.maxodiff.core.io;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MdParamsTest {

    @Test
    void theDefaultsAreValid() {
        MdParams p = MdParams.defaultParams();
        assertEquals(80, p.nRepetitions());
        assertEquals(20, p.nDiseases());
    }

    @Test
    void oneRepetitionAndOneDiseaseAreTheSmallestValidValues() {
        assertDoesNotThrow(() -> new MdParams(1, 1));
    }

    @Test
    void zeroOrNegativeRepetitionsAreRejectedWithTheValueInTheMessage() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new MdParams(0, 20));
        assertTrue(e.getMessage().contains("nRepetitions"));
        assertThrows(IllegalArgumentException.class, () -> new MdParams(-5, 20));
    }

    @Test
    void zeroOrNegativeDiseasesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MdParams(100, 0));
        assertThrows(IllegalArgumentException.class, () -> new MdParams(100, -1));
    }
}