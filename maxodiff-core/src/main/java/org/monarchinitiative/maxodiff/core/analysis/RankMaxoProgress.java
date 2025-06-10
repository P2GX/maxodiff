package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.concurrent.ConcurrentHashMap;

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
