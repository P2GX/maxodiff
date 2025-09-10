package org.monarchinitiative.maxodiff.html.results.maxoHpo;

public class RepetitionCellTooltipItem {
    private final String hpo;
    private final String percentage;

    public RepetitionCellTooltipItem(String hpo, String percentage) {
        this.hpo = hpo;
        this.percentage = percentage;
    }

    public String getHpo() {
        return hpo;
    }

    public String getPercentage() {
        return percentage;
    }
}
