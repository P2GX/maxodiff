package org.monarchinitiative.maxodiff.html.results.maxoHpo;

public class HpoTableCell {
    private final int count;
    private final double opacity;
    private final float mica;

    public HpoTableCell(int count, double opacity, float mica) {
        this.count = count;
        this.opacity = opacity;
        this.mica = mica;
    }

    public String getCount() {
        if (count == 0) {return ""; }
        return String.valueOf(count);
    }

    public double getOpacity() {
        return opacity;
    }

    public float getMica() {
        return mica;
    }

    /** This will be the color of the "square" in the HPO cell */
    public String getStyle() {
        if (count == 0) {
            float mica = getMica();
            return "rgba(0, 191, 255, " + mica + ")";
        }
        double opc = getOpacity();
        if (opc < 0.01) {
            opc = 0.01;
        }
        return "rgba(255, 165, 0, " + opc + ")";
    }

    public boolean isEmpty() {
        return count == 0;
    }

}
