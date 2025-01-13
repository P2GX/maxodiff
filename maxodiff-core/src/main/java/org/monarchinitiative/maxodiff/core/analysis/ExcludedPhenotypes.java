package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

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


    private final Map<TermId, Set<TermId>> hpoToMaxoTermIdMap;
    private final Map<TermId, Set<TermId>> maxoToHpoTermIdMap;

    /**
     * @param hpoToMaxoTermMap Map of HPO terms : Set of associated MAxO terms created using maxo_diagnostic_annotations file.
     */
    public ExcludedPhenotypes(Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap) {
        this.hpoToMaxoTermIdMap = MaxoHpoTermIdMaps.getHpoToMaxoTermIdMap(hpoToMaxoTermMap);
        this.maxoToHpoTermIdMap = MaxoHpoTermIdMaps.getMaxoToHpoTermIdMap(hpoToMaxoTermMap);
    }


    /**
     *
     * @param hpoId Existing HPO Term Id from phenopacket
     * @return Set of excluded HPO Ids for the HPO term, including any other existing HPO Ids in the phenopacket.
     * The other HPO Ids from the phenopacket are removed in getExcludedPhenotypes method.
     */
    public Set<TermId> getExcludedForHpoTerm(TermId hpoId) {
        Set<TermId> excluded;
        Set<TermId> associatedMaxoTermIds = hpoToMaxoTermIdMap.get(hpoId);
        List<Set<TermId>> maxoIdHpoIds = new ArrayList<>();
        if (associatedMaxoTermIds.size() == 1) {
            TermId maxoId = associatedMaxoTermIds.iterator().next();
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
    public Set<TermId> getExcludedPhenotypes(Sample samplePpkt) {
        Set<TermId> existingTerms = new HashSet<>(samplePpkt.presentHpoTermIds());
        existingTerms.addAll(samplePpkt.excludedHpoTermIds());
        Set<TermId> excludedPhenotypes = new HashSet<>();
        existingTerms.forEach(tid -> excludedPhenotypes.addAll(getExcludedForHpoTerm(tid)));
        excludedPhenotypes.removeAll(existingTerms);
        return excludedPhenotypes;
    }

    /**
     *
     * @param sets List of Sets of TermIds
     * @return The intersection of the sets, i.e. only the TermIds that are present in all the sets in the list.
     */
    public static Set<TermId> getIntersection(List<Set<TermId>> sets) {
        if (sets == null || sets.isEmpty()) {
            return new HashSet<>(); // Return an empty set if no input sets are provided
        }

        // Create a new set to avoid modifying the original sets
        Set<TermId> intersection = new HashSet<>(sets.getFirst()); // Retain only elements that are in all sets
        for (int i = 1; i < sets.size(); i++) {
            intersection.retainAll(sets.get(i));
        }

        return intersection;
    }
}
