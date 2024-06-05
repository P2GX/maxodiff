package org.monarchinitiative.maxodiff.core.analysis;

import com.fasterxml.jackson.annotation.JsonGetter;

import java.util.Collection;
import java.util.List;

public interface MaxodiffResult {

    static MaxodiffResult of(MaxoTermScore maxoTermScore, Collection<Frequencies> frequencies) {
        return new MaxodiffResultImpl(maxoTermScore, List.copyOf(frequencies));
    }

    @JsonGetter
    MaxoTermScore maxoTermScore();
    @JsonGetter
    List<Frequencies> frequencies();
}
