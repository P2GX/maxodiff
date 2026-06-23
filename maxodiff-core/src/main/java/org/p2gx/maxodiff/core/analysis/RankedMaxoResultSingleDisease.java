package org.p2gx.maxodiff.core.analysis;


/**
 * Record that contains parameters and final results of the maxodiff analysis
 *
 * @param maxoTerm The term id of the MAxO term associated with the results
 * @param nMaxoPhenotypes number of disease phenotypes ascertained by the MAxO term
 * @param totalIC total information content of disease phenotypes ascertained by the MAxO term
 *
 */
public record RankedMaxoResultSingleDisease(
        SimpleTerm targetDisease,
        SimpleTerm maxoTerm,
        int nMaxoPhenotypes,
        double totalIC,
        double maxoScore,
        RankedOmimTerm rankedOmimTerm) {



}

