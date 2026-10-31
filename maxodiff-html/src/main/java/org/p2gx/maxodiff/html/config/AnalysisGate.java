package org.p2gx.maxodiff.html.config;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/** Limits how many analyses run at once; later callers wait for a slot, up to a time limit. */
public class AnalysisGate {

    private final Semaphore slots;
    private final Duration maxWait;

    public AnalysisGate(int maxConcurrentAnalyses, Duration maxWait) {
        if (maxConcurrentAnalyses < 1) {
            throw new IllegalArgumentException("maxConcurrentAnalyses must be at least 1");
        }
        this.slots = new Semaphore(maxConcurrentAnalyses, true); // fair: first come, first served
        this.maxWait = maxWait;
    }

    /**
     * Takes a slot, waiting up to the configured time.
     *
     * @return a ticket to close when the analysis ends (use try-with-resources)
     * @throws ServerBusyException if no slot became free in time, or the thread was interrupted
     */
    public Ticket enter() {
        try {
            if (!slots.tryAcquire(maxWait.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new ServerBusyException();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServerBusyException();
        }
        return new Ticket(slots);
    }

    /** Gives the slot back when closed; closing more than once has no further effect. */
    public static final class Ticket implements AutoCloseable {
        private final Semaphore slots;
        private boolean released;

        private Ticket(Semaphore slots) {
            this.slots = slots;
        }

        @Override
        public synchronized void close() {
            if (!released) {
                released = true;
                slots.release();
            }
        }
    }
}