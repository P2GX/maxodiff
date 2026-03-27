package org.p2gx.maxodiff.core.analysis;

import java.util.*;

public class MaxoHpoTermIdMaps {

    /**
     * Make a map of HPO TermIds : Set of associated MAxO TermIds from HPO : MAxO term Map
     * @param hpoToMaxoTermMap Map of HPO terms : Set of associated MAxO terms created using maxo_diagnostic_annotations file.
     * @return Map of HPO TermIds : Set of associated MAxO TermIds
     */
    public static Map<String, Set<String>> getHpoToMaxoTermIdMap(Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap) {
        Map<String, Set<String>> hpoToMaxoTermIdMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : hpoToMaxoTermMap.entrySet()) {
            String hpoId = entry.getKey().termId();
            Set<SimpleTerm> maxoTerms = entry.getValue();
            for (SimpleTerm maxoTerm : maxoTerms) {
                String maxoId = maxoTerm.termId();
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
    public static Map<String, Set<String>> getMaxoToHpoTermIdMap(Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap) {
        Map<String, Set<String>> maxoToHpoTermIdMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : hpoToMaxoTermMap.entrySet()) {
            String hpoId = entry.getKey().termId();
            Set<SimpleTerm> maxoTerms = entry.getValue();
            for (SimpleTerm maxoTerm : maxoTerms) {
                String maxoId = maxoTerm.termId();
                maxoToHpoTermIdMap.computeIfAbsent(maxoId, k -> new HashSet<>()).add(hpoId);
            }
        }
        return maxoToHpoTermIdMap;
    }

}
