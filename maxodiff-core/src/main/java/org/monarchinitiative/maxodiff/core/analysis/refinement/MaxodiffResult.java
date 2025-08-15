package org.monarchinitiative.maxodiff.core.analysis.refinement;

import com.fasterxml.jackson.annotation.JsonGetter;
import org.monarchinitiative.maxodiff.core.analysis.Frequencies;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermScore;
import org.monarchinitiative.maxodiff.core.analysis.RankMaxoScore;

import java.util.Collection;
import java.util.List;

public interface MaxodiffResult {

    static MaxodiffResult of(MaxoTermScore maxoTermScore,
                             RankMaxoScore rankMaxoScore,
                             Collection<Frequencies> frequencies,
                             Collection<Frequencies> maxoFrequencies) {
        return new MaxodiffResultImpl(maxoTermScore, rankMaxoScore, List.copyOf(frequencies), List.copyOf(maxoFrequencies));
    }

    @JsonGetter
    MaxoTermScore maxoTermScore();
    @JsonGetter
    RankMaxoScore rankMaxoScore();
    @JsonGetter
    List<Frequencies> frequencies();
    @JsonGetter
    List<Frequencies> maxoFrequencies();
}
