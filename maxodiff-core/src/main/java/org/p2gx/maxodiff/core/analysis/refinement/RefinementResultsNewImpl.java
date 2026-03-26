package org.p2gx.maxodiff.core.analysis.refinement;

import java.util.Collection;
import java.util.Comparator;

record RefinementResultsNewImpl(Collection<MaxodiffResultNew> maxodiffResults) implements RefinementResultsNew {


    @Override
    public Collection<MaxodiffResultNew> maxodiffResults() {
        return maxodiffResults.stream()
                .sorted(Comparator.<MaxodiffResultNew>comparingDouble(mr -> mr.rankedMaxoResult().maxoScore()).reversed())
                .toList();
    }

}
