package org.monarchinitiative.maxodiff.core.analysis;

import java.util.List;
/**
 * Record that contains parameters and final results of the maxodiff analysis
 *
 * @param maxoTerm The term id of the MAxO term associated with the results
 * @param rankedOmimTermList OMIMs from the initial Phenomizer plus simulation with ranks
 * @param hpoTermIds Set of observed HPO terms discoverable by the MAxO term
 * @param maxoScore Final average score over the n simulations.
 *                  Scores are calculated using weighted rank differences between initial and final differential diagnosis disease lists
 * @param maxoDiagnoses List of DifferentialDiagnosis objects from the new MAxO term differential diagnosis calculations
 * @param hpoTermIdRepCtsMap Map of OMIM term id : Map of {HPO term id : count of occurrences in n simulations}.
 *                           Sorted in order of OMIM disease average rank change
 * @param maxoDiseaseAvgRankChangeMap Map of OMIM term id : List of [initial rank, rank change].
 *                                    Sorted in order of OMIM disease average rank change
 * @param minRankChange Minimum OMIM disease rank change
 * @param maxRankChange Maximum OMIM disease rank change
 */
public record RankedMaxoResult(
        SimpleTerm maxoTerm,
        List<RankedOmimTerm> rankedOmimTermList,
        List<CountedHpoTerm> hpoTermIds,
        double maxoScore) {



}


/*
        List<DifferentialDiagnosis> maxoDiagnoses, -- PROB NOT NEEDED
        Map<TermId, Map<TermId, Integer>> hpoTermIdRepCtsMap,

        int minRankChange,
        int maxRankChange) {
 */
