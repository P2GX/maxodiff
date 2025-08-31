package org.monarchinitiative.maxodiff.html.results;

public class HpoTableCell {
    private final int count;
    private final double opacity;

    public HpoTableCell(int count, double opacity) {
        this.count = count;
        this.opacity = opacity;
    }

    public int getCount() {
        return count;
    }

    public double getOpacity() {
        return opacity;
    }
}
