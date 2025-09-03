package org.monarchinitiative.maxodiff.html.results.maxoHpo;


import java.util.List;

/**
 * The contents of one cell of the first row of each MAxO table (with the Repetition counts
 */
public class RepetitionCell {
    private final String count;
    private final String background;
    private final String toolTipHeader;
    private final List<RepetitionCellTooltipItem> toolTipItems;

    public RepetitionCell(String count, String background, String toolTipHeader, List<RepetitionCellTooltipItem> toolTipItems) {
        this.count = count;
        this.background = background;
        this.toolTipHeader = toolTipHeader;
        this.toolTipItems = toolTipItems;
    }

    public String getCount() {
        return count;
    }

    public String getBackground() {
        return background;
    }

    public String getToolTipHeader() {
        return toolTipHeader;
    }

    public List<RepetitionCellTooltipItem> getToolTipItems() {
        return toolTipItems;
    }


}
