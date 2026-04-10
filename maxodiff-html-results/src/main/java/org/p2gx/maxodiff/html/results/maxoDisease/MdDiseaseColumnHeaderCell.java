package org.p2gx.maxodiff.html.results.maxoDisease;

import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;

public class MdDiseaseColumnHeaderCell {

    private final String maxoLabel;

    public MdDiseaseColumnHeaderCell(RankedMaxoResult result) {
        this.maxoLabel = result.maxoTerm().label();
    }

    public String getMaxoLabel() {
        return maxoLabel;
    }
}
