package org.monarchinitiative.maxodiff.html.results.maxoHpo;

public class HpoTableCell {
    private final int count;
    private final double opacity;

    public HpoTableCell(int count, double opacity) {
        this.count = count;
        this.opacity = opacity;
    }

    public String getCount() {
        if (count == 0) {return ""; }
        return String.valueOf(count);
    }

    public double getOpacity() {
        return opacity;
    }

    /** This will be the color of the "square" in the HPO cell */
    public String getStyle() {
        if (count == 0) { return ""; }
        double opc = getOpacity() * 0.5;
        if (opc < 0.01) {
            opc = 0.01;
        }
        return "rgba(33, 0, 90, " + opc + ")";
    }

    public boolean isEmpty() {
        return count == 0;
    }

}
