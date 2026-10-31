package org.p2gx.maxodiff.html.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Spring configuration for the compute resources shared by all MAxOdiff web requests.
 * <p>
 * The MAxOdiff analysis is CPU-intensive. If every request created its own thread pool, the
 * total number of compute threads would grow with the number of concurrent users. This class
 * instead defines a single, application-wide, bounded {@link ExecutorService} so that the
 * total number of analysis threads is capped regardless of how many requests arrive.
 */
@Configuration
public class ComputeConfig {

    /** Fraction of the available processors to use for analysis threads. */
    private static final double CORE_FRACTION = 0.10;

    /** Minimum number of worker threads, regardless of server size. */
    private static final int MIN_THREADS = 2;

    /** Number of analyses allowed to run at the same time (enforced in a later step). */
    static final int MAX_CONCURRENT_ANALYSES = 2;

    /** Upper bound on the tasks of one analysis: one per MAxO term, with headroom (a run uses up to 200). */
    private static final int MAX_TASKS_PER_ANALYSIS = 250;

    /** Tasks allowed to wait while all worker threads are busy: enough for the allowed analyses. */
    private static final int QUEUE_CAPACITY = MAX_CONCURRENT_ANALYSES * MAX_TASKS_PER_ANALYSIS;

    /**
     * Creates the shared executor used to run MAxOdiff analysis tasks.
     * <p>
     * The pool has a fixed size of {@code max(2, round(0.10 * availableProcessors))} threads.
     * On JDKs that are container-aware, {@link Runtime#availableProcessors()} reflects
     * CPU limits set by Docker or Kubernetes.
     * <p>
     * Tasks submitted while all threads are busy wait in a bounded queue of
     * {@value #QUEUE_CAPACITY} entries. When the queue is also full, submission fails with a
     * {@link java.util.concurrent.RejectedExecutionException} (the
     * {@link ThreadPoolExecutor.AbortPolicy}). Callers should catch it and report the server
     * as busy, for example with HTTP 429 or 503, instead of letting requests pile up.
     * <p>
     * Spring calls {@link ExecutorService#shutdown()} when the application context closes, so
     * running tasks are allowed to finish and no worker threads are left behind.
     *
     * @return the application-wide bounded executor for analysis tasks
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService maxodiffExecutor() {



        int cores = Runtime.getRuntime().availableProcessors();
        int n = Math.max(MIN_THREADS, (int) Math.round(cores * CORE_FRACTION));
        return new ThreadPoolExecutor(n, n, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadPoolExecutor.AbortPolicy());
    }
}
