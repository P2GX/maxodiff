package org.p2gx.maxodiff.core.analysis;

/** Ranks of OMIMs are initial Phenomizer analysis, and average rank after MaxoDiff procedure */
public record RankedOmimTerm(SimpleTerm omimTerm, int initialRank, float averageRank) {

    public float signedRankChange() {
        return averageRank - initialRank;
    }

}
