package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Record that contains parameters and final results of the maxodiff analysis
 *
 * @param maxoId The term id of the MAxO term associated with the results
 * @param initialOmimTermIds Set of OMIM term ids from the initial Phenomizer differential diagnosis
 * @param maxoOmimTermIds Set of OMIM term ids from the new differential diagnosis calculation using additional HPO terms discoverable by the MAxO term
 * @param discoverableObservedHpoTermIds Set of observed HPO terms discoverable by the MAxO term
 * @param chosenHpoTermCtsMap Map of HPO term ids : how many times each occurred in the n simulations
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
public record RankMaxoScore(
        TermId maxoId,
        Set<TermId> initialOmimTermIds,
        Set<TermId> maxoOmimTermIds,
        Set<TermId> discoverableObservedHpoTermIds,
        Map<TermId, Integer> chosenHpoTermCtsMap,
        Double maxoScore,
        List<DifferentialDiagnosis> maxoDiagnoses,
        Map<TermId, Map<TermId, Integer>> hpoTermIdRepCtsMap,
        Map<TermId, List<Integer>> maxoDiseaseAvgRankChangeMap,
        int minRankChange,
        int maxRankChange) {
}
