package org.monarchinitiative.maxodiff.core.analysis;

/** Ranks of OMIMs are initial LIRICAL analysis, and average rank after MaxoDiff procedure */
public record RankedOmimTerm(SimpleTerm maxoTerm, int initialRank, int averageRank) {

    public int signedRankChange() {
        return averageRank - initialRank;
    }

}
