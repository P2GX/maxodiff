package org.p2gx.maxodiff.core.analysis.refinement;

import com.fasterxml.jackson.annotation.JsonGetter;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;

import java.util.Collection;
import java.util.List;

public interface RefinementResults {

    static RefinementResults of(Collection<RankedMaxoResult> results) {
        return new RefinementResultsImpl(List.copyOf(results));
    }

    @JsonGetter
    Collection<RankedMaxoResult> maxodiffResults();
}
