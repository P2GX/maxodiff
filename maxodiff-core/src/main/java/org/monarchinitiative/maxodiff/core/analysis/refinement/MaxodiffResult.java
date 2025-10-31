package org.monarchinitiative.maxodiff.core.analysis.refinement;

import com.fasterxml.jackson.annotation.JsonGetter;
import org.monarchinitiative.maxodiff.core.analysis.Frequencies;
import org.monarchinitiative.maxodiff.core.analysis.RankMaxoScore;

import java.util.Collection;
import java.util.List;

public interface MaxodiffResult {

    static MaxodiffResult of(RankMaxoScore rankMaxoScore,
                             Collection<Frequencies> frequencies) {
        return new MaxodiffResultImpl(rankMaxoScore, List.copyOf(frequencies));
    }

    @JsonGetter
    RankMaxoScore rankMaxoScore();
    @JsonGetter
    List<Frequencies> frequencies();
}
