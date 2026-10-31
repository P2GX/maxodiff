package org.p2gx.maxodiff.html.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AnalysisGateTest {

    @Test
    void upToTheLimitMayEnter() {
        AnalysisGate gate = new AnalysisGate(2, Duration.ZERO);
        assertDoesNotThrow(() -> { gate.enter(); gate.enter(); });
    }

    @Test
    void oneMoreThanTheLimitIsTurnedAwayWhenThereIsNoWaiting() {
        AnalysisGate gate = new AnalysisGate(2, Duration.ZERO);
        gate.enter();
        gate.enter();
        assertThrows(ServerBusyException.class, gate::enter);
    }

    @Test
    void closingATicketFreesTheSlot() {
        AnalysisGate gate = new AnalysisGate(1, Duration.ZERO);
        AnalysisGate.Ticket first = gate.enter();
        assertThrows(ServerBusyException.class, gate::enter);
        first.close();
        assertDoesNotThrow(gate::enter);
    }

    @Test
    void closingTwiceReleasesOnlyOnce() {
        AnalysisGate gate = new AnalysisGate(1, Duration.ZERO);
        AnalysisGate.Ticket first = gate.enter();
        first.close();
        first.close();
        gate.enter();                                    // takes the one slot
        assertThrows(ServerBusyException.class, gate::enter); // a double release would have made room for a second
    }

    @Test
    void aWaitingCallerGetsInWhenASlotFrees() throws Exception {
        AnalysisGate gate = new AnalysisGate(1, Duration.ofSeconds(10));
        AnalysisGate.Ticket first = gate.enter();

        CompletableFuture<AnalysisGate.Ticket> waiting = CompletableFuture.supplyAsync(gate::enter);
        Thread.sleep(200);              // let the second caller start waiting
        assertFalse(waiting.isDone());
        first.close();

        assertNotNull(waiting.get(5, TimeUnit.SECONDS));
    }

    @Test
    void aCallerWhoWaitsTooLongIsTurnedAway() {
        AnalysisGate gate = new AnalysisGate(1, Duration.ofMillis(100));
        gate.enter();
        assertThrows(ServerBusyException.class, gate::enter);
    }

    @Test
    void zeroSlotsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AnalysisGate(0, Duration.ZERO));
    }
}