package org.p2gx.maxodiff.core.analysis;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DiseaseIdLabelMap {

    /**
     * Make a map of disease id : disease label
     * @param result RankedMaxoResult from maxodiff analysis.
     * @param hpoDiseases HpoDiseases object
     * @return Map of disease id : disease label.
     */
    public static Map<TermId, String> getDiseaseIdLabelMap(RankedMaxoResult result,
                                                           HpoDiseases hpoDiseases) {

        Map<TermId, String> diseaseIdLabelMap = new HashMap<>();
        for (RankedOmimTerm omimTerm : result.rankedOmimTermList()) {
            TermId diseaseId = omimTerm.omimTerm().tid();
            String diseaseLabel = hpoDiseases.diseaseById(diseaseId).get().diseaseName();
            diseaseIdLabelMap.put(diseaseId, diseaseLabel);
        }
        return diseaseIdLabelMap;
    }

}
