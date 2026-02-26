package org.monarchinitiative.maxodiff.html.results.maxoDisease;

import java.util.List;

public class MdDiseaseTableCell {
    private final double score;
    private final double opacity;
    private final String toolTipHeader;
    private final List<MaxoDiseaseCellTooltipItem> toolTipItems;

    public MdDiseaseTableCell(double score, double opacity, String toolTipHeader, List<MaxoDiseaseCellTooltipItem> toolTipItems) {
        this.score = score;
        this.opacity = opacity;
        this.toolTipHeader = toolTipHeader;
        this.toolTipItems = toolTipItems;
    }

    public String getScore() {
        if (score == 0) {return ""; }
        return String.format("%.2f", score);
    }

    public double getOpacity() {
        return opacity;
    }

    /** This will be the color of the "square" in the HPO cell */
    public String getStyle() {
        if (score == 0) { return ""; }
        double opc = getOpacity() * 0.5;
        if (opc < 0.01) {
            opc = 0.01;
        }
        return "rgba(220, 20, 60, " + opc + ")";
    }

    public String getToolTipHeader() {
        return toolTipHeader;
    }

    public List<MaxoDiseaseCellTooltipItem> getToolTipItems() {
        return toolTipItems;
    }

    public boolean isEmpty() {
        return score == 0;
    }
}
