package org.p2gx.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code RankMaxoProgress} provides a simple thread-safe mechanism to track
 * progress across multiple MAXO term ranking tasks.
 *
 * <p>Each task corresponds to a specific {@link TermId} representing a MAXO term.
 * Progress is reported as a fraction of completed tasks relative to the total
 * number of MAXO terms.</p>
 *
 * <p>This class is intended for use in multi-threaded analyses where progress
 * updates occur concurrently.</p>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * RankMaxoProgress progress = new RankMaxoProgress(totalMaxoTerms);
 * progress.updateProgress(maxoId, 1.0); // mark one term as complete
 * double percentDone = progress.getTotalProgress() * 100;
 * }</pre>
 *
 * @see TermId
 */
public class RankMaxoProgress {

    private final ConcurrentHashMap<TermId, Double> taskProgress = new ConcurrentHashMap<>();
    private final int nMaxoTerms;

    public RankMaxoProgress(int nMaxoTerms) {
        this.nMaxoTerms = nMaxoTerms;
    }

    public void updateProgress(TermId maxoId, double progress) {
        taskProgress.put(maxoId, progress);
    }

    public double getTotalProgress() {
        if (taskProgress.isEmpty()) return 0;
        return (double) taskProgress.size() / nMaxoTerms;
    }

    public void reset() {
        taskProgress.clear();
    }
}
