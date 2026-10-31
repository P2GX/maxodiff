package org.p2gx.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;

class MaxoTermEvaluatorTest {

    @Test
    void zeroRepetitionsIsRejectedWithAClearMessage() {
        assertThrows(IllegalArgumentException.class, () ->
                new MaxoTermEvaluator(null, 0, null, null, List.of(), Set.of(), null, List.of()));
    }

    @Test
    void negativeRepetitionsIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new MaxoTermEvaluator(null, -3, null, null, List.of(), Set.of(), null, List.of()));
    }
}