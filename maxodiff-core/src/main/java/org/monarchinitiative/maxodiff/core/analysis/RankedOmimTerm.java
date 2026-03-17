package org.monarchinitiative.maxodiff.core.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** Ranks of OMIMs are initial Phenomizer analysis, and average rank after MaxoDiff procedure */
public record RankedOmimTerm(@JsonUnwrapped SimpleTerm omimTerm,
                             @JsonIgnore int initialRank,
                             float averageRank) {

    public float signedRankChange() {
        return averageRank - initialRank;
    }

}
