package org.monarchinitiative.maxodiff.core.analysis.refinement;

import com.fasterxml.jackson.annotation.JsonGetter;
import org.monarchinitiative.maxodiff.core.analysis.Frequencies;
import org.monarchinitiative.maxodiff.core.analysis.RankMaxoScore;
import org.monarchinitiative.maxodiff.core.analysis.RankedMaxoResult;

import java.util.Collection;
import java.util.List;

public interface MaxodiffResultNew {

    static MaxodiffResultNew of(RankedMaxoResult rankedMaxoResult) {
        return new MaxodiffResultNewImpl(rankedMaxoResult);
    }

    @JsonGetter
    RankedMaxoResult rankedMaxoResult();
}
