package org.monarchinitiative.maxodiff.html.results.maxoDisease;

import org.monarchinitiative.maxodiff.core.analysis.RankedMaxoResult;

public class MdDiseaseColumnHeaderCell {

    private final String maxoLabel;

    public MdDiseaseColumnHeaderCell(RankedMaxoResult result) {
        this.maxoLabel = result.maxoTerm().termLabel();
    }

    public String getMaxoLabel() {
        return maxoLabel;
    }
}
