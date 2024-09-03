package org.monarchinitiative.maxodiff.core.analysis;

import com.fasterxml.jackson.annotation.JsonGetter;

import java.util.Collection;
import java.util.List;

public interface MaxodiffResult {

    static MaxodiffResult of(MaxoTermScore maxoTermScore,
                             Collection<Frequencies> frequencies,
                             Collection<Frequencies> maxoFrequencies) {
        return new MaxodiffResultImpl(maxoTermScore, List.copyOf(frequencies), List.copyOf(maxoFrequencies));
    }

    @JsonGetter
    MaxoTermScore maxoTermScore();
    @JsonGetter
    List<Frequencies> frequencies();
    @JsonGetter
    List<Frequencies> maxoFrequencies();
}
