package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.maxodiff.core.analysis.SimpleTerm;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class calculates the excluded phenotypes, i.e. phenotypes that can be ascertained by MAxO terms,
 * but are not included in the existing phenotypes in the phenopacket.
 * Optionally, we can assume that certain phenotypes would have been ascertained but were not mentioned.
 * For instance, if the phenopacket says we observed Ventricular septum defect, then if the HPOAs also has atrial
 * septal defect for the disease, we can assume that the patient does NOT have ASD because we would have seen it with
 * echocardiography (which you need to diagnosis both ASD and VSD).
 * This is not dependent on the disease diagnosis.
 */
public class ExcludedPhenotypes {


    private final Map<String, Set<String>> hpoToMaxoTermIdMap;
    private final Map<String, Set<String>> maxoToHpoTermIdMap;

    /**
     * @param hpoToMaxoTermIdMap Map of HPO term ids : Set of associated MAxO term ids created using maxo_diagnostic_annotations file.
     * @param maxoToHpoTermIdMap Map of MAxO term ids : Set of associated HPO term ids created using maxo_diagnostic_annotations file.
     */
    public ExcludedPhenotypes(Map<String, Set<String>> hpoToMaxoTermIdMap,
                              Map<String, Set<String>> maxoToHpoTermIdMap) {
        this.hpoToMaxoTermIdMap = hpoToMaxoTermIdMap;
        this.maxoToHpoTermIdMap = maxoToHpoTermIdMap;
    }


    /**
     *
     * @param hpoId Existing HPO Term Id from phenopacket
     * @return Set of excluded HPO Ids for the HPO term, including any other existing HPO Ids in the phenopacket.
     * The other HPO Ids from the phenopacket are removed in getExcludedPhenotypes method.
     */
    public Set<String> getExcludedForHpoTerm(String hpoId) {
        Set<String> excluded;
        Set<String> associatedMaxoTermIds = hpoToMaxoTermIdMap.get(hpoId);
        List<Set<String>> maxoIdHpoIds = new ArrayList<>();
        if (associatedMaxoTermIds == null) {
            excluded = Set.of();
        } else if (associatedMaxoTermIds.size() == 1) {
            String maxoId = associatedMaxoTermIds.iterator().next();
            excluded = maxoToHpoTermIdMap.get(maxoId);
        } else if (associatedMaxoTermIds.isEmpty()) {
            excluded = Set.of();
        } else {
            associatedMaxoTermIds.forEach(mid -> maxoIdHpoIds.add(maxoToHpoTermIdMap.get(mid)));
            // get intersection of sets
            excluded = getIntersection(maxoIdHpoIds);
        }
        return excluded;
    }

    /**
     *
     * @param samplePpkt Input phenopacket with present and excluded HPO terms.
     * @return Set of excluded phenotypes. These are phenotypes that can be ascertained by MAxO terms,
     *  but are not included in the existing phenotypes in the phenopacket.
     */
    public Set<String> getExcludedPhenotypes(PpktSample samplePpkt) {
        Set<String> existingTerms = new HashSet<>();
        existingTerms.addAll(samplePpkt.observedHpoTerms().stream().map(SimpleTerm::termId).collect(Collectors.toSet()));
        existingTerms.addAll(samplePpkt.excludedHpoTerms().stream().map(SimpleTerm::termId).collect(Collectors.toSet()));
        Set<String> excludedPhenotypes = new HashSet<>();
        existingTerms.forEach(tid -> excludedPhenotypes.addAll(getExcludedForHpoTerm(tid)));
        excludedPhenotypes.removeAll(existingTerms);
        return excludedPhenotypes;
    }

    /**
     *
     * @param sets List of Sets of TermIds
     * @return The intersection of the sets, i.e. only the TermIds that are present in all the sets in the list.
     */
    public static Set<String> getIntersection(List<Set<String>> sets) {
        if (sets == null || sets.isEmpty()) {
            return new HashSet<>(); // Return an empty set if no input sets are provided
        }

        // Create a new set to avoid modifying the original sets
        Set<String> intersection = new HashSet<>(sets.getFirst()); // Retain only elements that are in all sets
        for (int i = 1; i < sets.size(); i++) {
            intersection.retainAll(sets.get(i));
        }

        return intersection;
    }
}
