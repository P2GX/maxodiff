package org.monarchinitiative.maxodiff.html.results;

import java.util.ArrayList;
import java.util.List;

public class MaxoHtmlResultTableRow {
    private String diseaseName;

    // Parent cell
    private String parentBackground;
    private String parentValue;
    private String parentLabel;

    // HPO cells
    private List<HpoTableCell> hpoCells = new ArrayList<>();

    private String computeParentBackground(int deltaR) {
        if (deltaR < 0) {
            // green gradient for improvement
            int green = Math.min(255, 50 + Math.abs(deltaR) * 10);
            return "rgba(0," + green + ",0,0.55)";
        } else if (deltaR > 0) {
            // red gradient for decline
            int red = Math.min(255, 50 + deltaR * 10);
            return "rgba(" + red + ",0,0,0.55)";
        } else {
            return "rgba(200,200,200,0.55)";
        }
    }
}
