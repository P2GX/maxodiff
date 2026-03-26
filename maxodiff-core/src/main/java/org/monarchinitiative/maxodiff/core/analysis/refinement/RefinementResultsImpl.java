package org.monarchinitiative.maxodiff.core.analysis.refinement;

import org.monarchinitiative.maxodiff.core.analysis.RankedMaxoResult;

import java.util.Collection;
import java.util.Comparator;

record RefinementResultsImpl(Collection<RankedMaxoResult> maxodiffResults) implements RefinementResults {


    @Override
    public Collection<RankedMaxoResult> maxodiffResults() {
        return maxodiffResults. stream()
                .sorted(Comparator.<RankedMaxoResult>comparingDouble(mr -> mr.maxoScore()).reversed())
                .toList();
    }

}
