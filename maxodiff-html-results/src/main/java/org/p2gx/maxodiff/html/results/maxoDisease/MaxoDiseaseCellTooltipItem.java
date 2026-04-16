package org.p2gx.maxodiff.html.results.maxoDisease;

public class MaxoDiseaseCellTooltipItem {
    private final String maxo;
    private final String score;

    public MaxoDiseaseCellTooltipItem(String maxo, String score) {
        this.maxo = maxo;
        this.score = score;
    }

    public String getMaxo() {
        return maxo;
    }

    public String getScore() {
        return score;
    }
}
