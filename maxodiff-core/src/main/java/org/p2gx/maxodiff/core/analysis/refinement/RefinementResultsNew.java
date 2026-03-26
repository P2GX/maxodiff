package org.p2gx.maxodiff.core.analysis.refinement;

import com.fasterxml.jackson.annotation.JsonGetter;

import java.util.Collection;
import java.util.List;

public interface RefinementResultsNew {

    static RefinementResultsNew of(Collection<MaxodiffResultNew> results) {
        return new RefinementResultsNewImpl(List.copyOf(results));
    }

    @JsonGetter
    Collection<MaxodiffResultNew> maxodiffResults();
}
