package org.monarchinitiative.maxodiff.html.results.maxoDisease;

import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;

public class MaxoDiseaseColumnHeaderCell {

    String maxoLabel;

    public MaxoDiseaseColumnHeaderCell(MaxodiffResult result, BiometadataService biometadataService) {
        String maxoId = result.rankMaxoScore().maxoId().toString();
        this.maxoLabel = biometadataService.maxoLabel(maxoId).orElse("unknown");
    }

    public String getMaxoLabel() {
        return maxoLabel;
    }
}
