package org.monarchinitiative.maxodiff.core.analysis;

import com.fasterxml.jackson.annotation.JsonGetter;

import java.util.Collection;
import java.util.List;

public interface RefinementResults {

    static RefinementResults of(Collection<MaxodiffResult> results) {
        return new RefinementResultsImpl(List.copyOf(results));
    }

    @JsonGetter
    Collection<MaxodiffResult> maxodiffResults();
}
