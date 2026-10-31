package org.p2gx.maxodiff.html.config;

import org.p2gx.maxodiff.html.config.AnalysisGate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ComputeConfig {

    /** Fraction of the available processors to use for analysis threads. */
    private static final double CORE_FRACTION = 0.10;

    /** Minimum number of worker threads, regardless of server size. */
    private static final int MIN_THREADS = 2;

    /** Default number of analyses allowed to run at the same time (property: maxodiff.max-concurrent-analyses). */
    private static final int MAX_CONCURRENT_ANALYSES = 2;

    /** Upper bound on the tasks of one analysis: one per MAxO term, with headroom (a run uses up to 200). */
    private static final int MAX_TASKS_PER_ANALYSIS = 250;

    /**
     * The application-wide pool for analysis tasks, sized at max(2, round(10% of the available processors)).
     * Waiting tasks queue up to {@code maxConcurrent * 250}; beyond that submission fails with a
     * {@link java.util.concurrent.RejectedExecutionException}. Spring shuts the pool down when the context closes.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService maxodiffExecutor(
            @Value("${maxodiff.max-concurrent-analyses:" + MAX_CONCURRENT_ANALYSES + "}") int maxConcurrent) {
        int cores = Runtime.getRuntime().availableProcessors();
        int n = Math.max(MIN_THREADS, (int) Math.round(cores * CORE_FRACTION));
        return new ThreadPoolExecutor(n, n, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(maxConcurrent * MAX_TASKS_PER_ANALYSIS),
                new ThreadPoolExecutor.AbortPolicy());
    }

    /** Admits at most {@code maxConcurrent} analyses at a time; others wait, then get an HTTP 503. */
    @Bean
    public AnalysisGate analysisGate(
            @Value("${maxodiff.max-concurrent-analyses:" + MAX_CONCURRENT_ANALYSES + "}") int maxConcurrent,
            @Value("${maxodiff.admission-wait-seconds:60}") long waitSeconds) {
        return new AnalysisGate(maxConcurrent, Duration.ofSeconds(waitSeconds));
    }
}