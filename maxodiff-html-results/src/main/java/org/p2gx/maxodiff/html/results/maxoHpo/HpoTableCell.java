package org.p2gx.maxodiff.html.results.maxoHpo;

public class HpoTableCell {
    private final int count;
    private final double opacity;
    private final float mica;
    private final double maxMica;

    public HpoTableCell(int count, double opacity, float mica, double maxMica) {
        this.count = count;
        this.opacity = opacity;
        this.mica = mica;
        this.maxMica = maxMica;
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

    public double getMaxMica() {
        return maxMica;
    }

    /** This will be the color of the "square" in the HPO cell */
    public String getStyle() {
        double opc = getOpacity();
        if (opc < 0.01) {
            opc = 0.01;
        }
        return "rgba(255, 127, 80, " + opc + ")";
    }

    public boolean isEmpty() {
        return count == 0;
    }

}
