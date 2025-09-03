package org.monarchinitiative.maxodiff.html.results.maxoDisease;

public class MaxoDiseaseTableCell {
    //TODO: show maxo term rank as text, and color-code accordingly
    private final double score;
    private final double opacity;

    public MaxoDiseaseTableCell(double score, double opacity) {
        this.score = score;
        this.opacity = opacity;
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
        return "rgba(33, 0, 90, " + opc + ")";
    }

    public boolean isEmpty() {
        return score == 0;
    }
}
