package org.monarchinitiative.maxodiff.core.analysis.refinement;

import java.util.Collection;
import java.util.Comparator;

record RefinementResultsImpl(Collection<MaxodiffResult> maxodiffResults) implements RefinementResults {


    @Override
    public Collection<MaxodiffResult> maxodiffResults() {
        return maxodiffResults.stream()
                .sorted(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.rankMaxoScore().maxoScore()).reversed())
                .toList();
    }

}
