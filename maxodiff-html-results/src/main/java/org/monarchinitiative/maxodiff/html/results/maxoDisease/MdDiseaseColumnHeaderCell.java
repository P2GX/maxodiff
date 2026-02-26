package org.monarchinitiative.maxodiff.html.results.maxoDisease;

import org.monarchinitiative.maxodiff.core.analysis.RankedMaxoResult;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;

public class MdDiseaseColumnHeaderCell {

    String maxoLabel;

    public MdDiseaseColumnHeaderCell(RankedMaxoResult result) {
        String maxoId = result.maxoTerm().termId();
        this.maxoLabel = result.maxoTerm().termLabel();
    }

    public String getMaxoLabel() {
        return maxoLabel;
    }
}
