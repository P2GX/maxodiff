package org.monarchinitiative.maxodiff.core.analysis.refinement;

import org.apache.commons.math3.random.EmpiricalDistribution;
import org.monarchinitiative.maxodiff.core.analysis.DiseaseTermCount;
import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermScore;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;

class AnalysisUtils {

    private AnalysisUtils() {}

    /**
     *
     * @param diseases List of Hpo diseases
     * @return Map of HPO Term Id and List of HpoFrequency objects.
     */
    static Map<TermId, List<HpoFrequency>> getHpoTermCounts(List<HpoDisease> diseases) {
        // Collect HPO terms and frequencies for the target m diseases
        DiseaseTermCount diseaseTermCount = DiseaseTermCount.of(diseases);
        return diseaseTermCount.hpoTermCounts();
    }

    /**
     *
     * @param fullHpoToMaxoTermMap Map of all HPO -> MAXO TermId set mappings from maxo_diagnostic_annotations file.
     * @param hpoTermIds Set of HPO TermIds associated with the subset of m diseases.
     * @return Map of HPO -> MAXO TermId set mappings for the subset of m diseases.
     */
    static Map<TermId, Set<TermId>> makeHpoToMaxoTermIdMap(Map<TermId, Set<TermId>> fullHpoToMaxoTermMap,
                                                           Set<TermId> hpoTermIds) {
        Map<TermId, Set<TermId>> hpoToMaxoTermIdMap = new HashMap<>();
        for (TermId hpoId : hpoTermIds) {
            for (TermId hpoTermId : fullHpoToMaxoTermMap.keySet()) {
                if (hpoTermId.equals(hpoId)) {
                    Set<TermId> maxoTermIds = fullHpoToMaxoTermMap.get(hpoTermId);
                    hpoToMaxoTermIdMap.put(hpoTermId, maxoTermIds);
                    break;
                }
            }
        }
        return hpoToMaxoTermIdMap;
    }

    /**
     *
     * @param ontology HPO Ontology.
     * @param hpoToMaxoTermMap Map of HPO -> MAXO TermId set mappings for the subset of m diseases.
     * @return Map of MAXO -> HPO TermId set mappings for the subset of m diseases. HPO ancestors are removed.
     */
    static Map<TermId, Set<TermId>> makeMaxoToHpoTermIdMap(MinimalOntology ontology, Map<TermId, Set<TermId>> hpoToMaxoTermMap) {
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = new HashMap<>();
        for (Map.Entry<TermId, Set<TermId>> entry : hpoToMaxoTermMap.entrySet()) {
            TermId hpoTermId = entry.getKey();
            Set<TermId> maxoTermIds = entry.getValue();
            for (TermId maxoTermId : maxoTermIds) {
                if (!maxoToHpoTermIdMap.containsKey(maxoTermId)) {
                    maxoToHpoTermIdMap.put(maxoTermId, new HashSet<>(Collections.singleton(hpoTermId)));
                } else {
                    Set<TermId> hpoTermIds = maxoToHpoTermIdMap.get(maxoTermId);
                    hpoTermIds.add(hpoTermId);
                    maxoToHpoTermIdMap.replace(maxoTermId, hpoTermIds);
                }
            }
        }
        //TODO: removing ancestors possibly incorrect for excluded HPO features
        for (Map.Entry<TermId, Set<TermId>> e : maxoToHpoTermIdMap.entrySet()) {
            // Remove HPO ancestor term Ids from list
            TermId mId = e.getKey();
            Set<TermId> hpoIdSet = new HashSet<>(e.getValue());
            for (TermId hpoId : e.getValue()) {
                try {
                    for (TermId ancestor : ontology.graph().getAncestors(hpoId)) {
                        hpoIdSet.remove(ancestor);
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
