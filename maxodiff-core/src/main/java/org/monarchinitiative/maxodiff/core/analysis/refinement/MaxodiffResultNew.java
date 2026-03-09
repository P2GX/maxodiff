package org.monarchinitiative.maxodiff.core.analysis.refinement;

import com.fasterxml.jackson.annotation.JsonGetter;
import org.monarchinitiative.maxodiff.core.analysis.RankedMaxoResult;

public interface MaxodiffResultNew {

    static MaxodiffResultNew of(RankedMaxoResult rankedMaxoResult) {
        return new MaxodiffResultNewImpl(rankedMaxoResult);
    }

    @JsonGetter
    RankedMaxoResult rankedMaxoResult();
}
