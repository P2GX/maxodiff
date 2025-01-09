package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class MaxoHpoTermIdMaps {

    /**
     * Make a map of HPO TermIds : Set of associated MAxO TermIds from HPO : MAxO term Map
     * @param hpoToMaxoTermMap Map of HPO terms : Set of associated MAxO terms created using maxo_diagnostic_annotations file.
     * @return Map of HPO TermIds : Set of associated MAxO TermIds
     */
    public static Map<TermId, Set<TermId>> getHpoToMaxoTermIdMap(Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap) {
        Map<TermId, Set<TermId>> hpoToMaxoTermIdMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : hpoToMaxoTermMap.entrySet()) {
            TermId hpoId = entry.getKey().tid();
            Set<SimpleTerm> maxoTerms = entry.getValue();
            for (SimpleTerm maxoTerm : maxoTerms) {
                TermId maxoId = maxoTerm.tid();
                if (!hpoToMaxoTermIdMap.containsKey(hpoId)) {
                    hpoToMaxoTermIdMap.put(hpoId, new HashSet<>(Collections.singleton(maxoId)));
                } else {
                    Set<TermId> maxoIdSet = hpoToMaxoTermIdMap.get(hpoId);
                    maxoIdSet.add(maxoId);
                    hpoToMaxoTermIdMap.replace(hpoId, maxoIdSet);
                }
            }
        }
        return hpoToMaxoTermIdMap;
    }

    /**
     * Make a map of MAxO TermIds : Set of associated HPO TermIds from HPO : MAxO term Map
     * @param hpoToMaxoTermMap Map of HPO terms : Set of associated MAxO terms created using maxo_diagnostic_annotations file.
     * @return Map of MAxO TermIds : Set of associated HPO TermIds
     */
    public static Map<TermId, Set<TermId>> getMaxoToHpoTermIdMap(Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap) {
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : hpoToMaxoTermMap.entrySet()) {
            TermId hpoId = entry.getKey().tid();
            Set<SimpleTerm> maxoTerms = entry.getValue();
            for (SimpleTerm maxoTerm : maxoTerms) {
                TermId maxoId = maxoTerm.tid();
                if (!maxoToHpoTermIdMap.containsKey(maxoId)) {
                    maxoToHpoTermIdMap.put(maxoId, new HashSet<>(Collections.singleton(hpoId)));
                } else {
                    Set<TermId> hpoIdSet = maxoToHpoTermIdMap.get(maxoId);
                    hpoIdSet.add(hpoId);
                    maxoToHpoTermIdMap.replace(maxoId, hpoIdSet);
                }
            }
        }
        return maxoToHpoTermIdMap;
    }

}
