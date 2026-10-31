package org.p2gx.maxodiff.core.model;

import org.junit.jupiter.api.Test;
import org.p2gx.maxodiff.core.MaxodiffRuntimeException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class RankMaxoTest {

    @Test
    void thePoolIsTheHyperparameterWhenEnoughDiseasesAreAvailable() {
        assertEquals(RankMaxo.N_SAMPLING_DIAGNOSES, RankMaxo.samplingPoolSize(8000));
        assertEquals(RankMaxo.N_SAMPLING_DIAGNOSES, RankMaxo.samplingPoolSize(RankMaxo.N_SAMPLING_DIAGNOSES));
    }

    @Test
    void thePoolIsReducedWhenFewerDiseasesAreAvailable() {
        assertEquals(120, RankMaxo.samplingPoolSize(120));
        assertEquals(0, RankMaxo.samplingPoolSize(0));
    }

    @Test
    void aSuccessfulFutureGivesItsValue() {
        assertEquals("ok", RankMaxo.resultOf(CompletableFuture.completedFuture("ok")));
    }

    @Test
    void aFailedTaskIsReportedWithItsCause() {
        Future<String> failed = CompletableFuture.failedFuture(new IllegalStateException("boom"));

        MaxodiffRuntimeException e = assertThrows(MaxodiffRuntimeException.class, () -> RankMaxo.resultOf(failed));

        assertInstanceOf(IllegalStateException.class, e.getCause());
        assertTrue(e.getMessage().contains("boom"));
    }

    @Test
    void interruptionIsReportedAndTheInterruptFlagRestored() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(MaxodiffRuntimeException.class, () -> RankMaxo.resultOf(new CompletableFuture<String>()));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted(); // clear the flag so other tests are unaffected
        }
    }
}