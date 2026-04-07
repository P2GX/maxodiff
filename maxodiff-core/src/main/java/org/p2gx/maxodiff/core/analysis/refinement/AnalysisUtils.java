package org.p2gx.maxodiff.core.analysis.refinement;

import org.p2gx.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

class AnalysisUtils {

    private AnalysisUtils() {}

    /**
     *
     * @param fullMap Map of all HPO -> MAXO TermId set mappings from maxo_diagnostic_annotations file.
     * @param hpoTermIds Set of HPO TermIds associated with the subset of m diseases.
     * @return Map of HPO -> MAXO TermId set mappings for the subset of m diseases.
     */
    static Map<String, Set<String>> makeHpoToMaxoTermIdMap(Map<String, Set<String>> fullMap,
                                                           Set<String> hpoTermIds) {
        Map<String, Set<String>> filteredMap = new HashMap<>();

        for (String hpoId : hpoTermIds) {
            if (fullMap.containsKey(hpoId)) {
                filteredMap.put(hpoId, fullMap.get(hpoId));
            }
        }

        return filteredMap;
    }

    /**
     *
     * @param ontology HPO Ontology.
     * @param hpoToMaxoTermMap Map of HPO -> MAXO TermId set mappings for the subset of m diseases.
     * @return Map of MAXO -> HPO TermId set mappings for the subset of m diseases. HPO ancestors are removed.
     */
    static Map<String, Set<String>> makeMaxoToHpoTermIdMap(MinimalOntology ontology, Map<String, Set<String>> hpoToMaxoTermMap) {
        Map<String, Set<String>> maxoToHpoTermIdMap = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : hpoToMaxoTermMap.entrySet()) {
            String hpoTermId = entry.getKey();
            Set<String> maxoTermIds = entry.getValue();
            for (String maxoTermId : maxoTermIds) {
                if (!maxoToHpoTermIdMap.containsKey(maxoTermId)) {
                    maxoToHpoTermIdMap.put(maxoTermId, new HashSet<>(Collections.singleton(hpoTermId)));
                } else {
                    Set<String> hpoTermIds = maxoToHpoTermIdMap.get(maxoTermId);
                    hpoTermIds.add(hpoTermId);
                    maxoToHpoTermIdMap.replace(maxoTermId, hpoTermIds);
                }
            }
        }
        //TODO: removing ancestors possibly incorrect for excluded HPO features
        for (Map.Entry<String, Set<String>> e : maxoToHpoTermIdMap.entrySet()) {
            // Remove HPO ancestor term Ids from list
            String mId = e.getKey();
            Set<String> hpoIdSet = new HashSet<>(e.getValue());
            for (String hpoId : e.getValue()) {
                try {
                    for (TermId ancestor : ontology.graph().getAncestors(TermId.of(hpoId))) {
                        hpoIdSet.remove(ancestor.getValue());
                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
            maxoToHpoTermIdMap.replace(mId, hpoIdSet);
        }
        return maxoToHpoTermIdMap;
    }
}
