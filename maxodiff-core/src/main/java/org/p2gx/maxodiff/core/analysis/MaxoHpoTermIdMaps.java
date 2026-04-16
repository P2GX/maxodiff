package org.p2gx.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class MaxoHpoTermIdMaps {

    /**
     * Make a map of HPO TermIds : Set of associated MAxO TermIds from HPO : MAxO term Map
     * @param hpoToMaxoTermMap Map of HPO terms : Set of associated MAxO terms created using maxo_diagnostic_annotations file.
     * @return Map of HPO TermIds : Set of associated MAxO TermIds
     */
    public static Map<TermId, Set<TermId>> getHpoToMaxoTermIdMap(Map<MySimpleTerm, Set<MySimpleTerm>> hpoToMaxoTermMap) {
        Map<TermId, Set<TermId>> hpoToMaxoTermIdMap = new HashMap<>();
        for (Map.Entry<MySimpleTerm, Set<MySimpleTerm>> entry : hpoToMaxoTermMap.entrySet()) {
            TermId hpoId = entry.getKey().tid();
            Set<MySimpleTerm> maxoTerms = entry.getValue();
            for (MySimpleTerm maxoTerm : maxoTerms) {
                TermId maxoId = maxoTerm.tid();
                hpoToMaxoTermIdMap.computeIfAbsent(hpoId, k -> new HashSet<>()).add(maxoId);
            }
        }
        return hpoToMaxoTermIdMap;
    }

    /**
     * Make a map of MAxO TermIds : Set of associated HPO TermIds from HPO : MAxO term Map
     * @param hpoToMaxoTermMap Map of HPO terms : Set of associated MAxO terms created using maxo_diagnostic_annotations file.
     * @return Map of MAxO TermIds : Set of associated HPO TermIds
     */
    public static Map<TermId, Set<TermId>> getMaxoToHpoTermIdMap(Map<MySimpleTerm, Set<MySimpleTerm>> hpoToMaxoTermMap) {
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = new HashMap<>();
        for (Map.Entry<MySimpleTerm, Set<MySimpleTerm>> entry : hpoToMaxoTermMap.entrySet()) {
            TermId hpoId = entry.getKey().tid();
            Set<MySimpleTerm> maxoTerms = entry.getValue();
            for (MySimpleTerm maxoTerm : maxoTerms) {
                TermId maxoId = maxoTerm.tid();
                maxoToHpoTermIdMap.computeIfAbsent(maxoId, k -> new HashSet<>()).add(hpoId);
            }
        }
        return maxoToHpoTermIdMap;
    }

}
