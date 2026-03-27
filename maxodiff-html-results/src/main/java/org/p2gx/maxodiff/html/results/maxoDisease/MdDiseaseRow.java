package org.p2gx.maxodiff.html.results.maxoDisease;

import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;
import org.p2gx.maxodiff.core.analysis.RankedOmimTerm;

import java.util.ArrayList;
import java.util.List;

public class MdDiseaseRow {
    private final String omimId;
    private final String omimLabel;
    private final Double score;
    private final Double topScore;
    private final List<MdDiseaseTableCell> cells;

    public MdDiseaseRow(String omimId, String omimLabel, Double score, Double topScore, List<MdDiseaseTableCell> cells) {
        this.omimId = omimId;
        this.omimLabel = omimLabel;
        this.score = score;
        this.topScore = topScore;
        this.cells = cells;
    }


    public static List<MdDiseaseRow> createMaxoDiseaseRows(List<RankedMaxoResult> results) {

        List<MdDiseaseRow> rows = new ArrayList<>();
        for (RankedOmimTerm rankedOmimTerm : results.getFirst().rankedOmimTermList()) {
            String omimId = rankedOmimTerm.omimTerm().termId();
            String omimLabel = rankedOmimTerm.omimTerm().termLabel();
            List<MdDiseaseTableCell> cells = new ArrayList<>();
            double score = 0.;
            double topScore = results.getFirst().maxoScore();
            String tooltipHeader = "MAxO Term Score";
            List<MaxoDiseaseCellTooltipItem> tooltipItems = new ArrayList<>();
            for (RankedMaxoResult result : results) {
                if (!result.frequencies().isEmpty()) {
                    String firstMaxoDiseaseId = result.frequencies().getFirst().omimId();
                    if (firstMaxoDiseaseId.equals(omimId)) {
                        String maxoLabel = result.maxoTerm().termLabel();
                        score = result.maxoScore();
                        tooltipItems.add(new MaxoDiseaseCellTooltipItem(maxoLabel, String.format("%.2f", score)));
                    } else {
                        score = 0.;
                    }
                    double opacity = score / topScore;
                    cells.add(new MdDiseaseTableCell(score, opacity, tooltipHeader, tooltipItems));
                }
            }
            rows.add(new MdDiseaseRow(omimId, omimLabel, score, topScore, cells));

        }
        return rows;
    }

    public String getStyle() {
        double opacity = (double) score / topScore;
        return "rgba(255, 0, 0, " + opacity + ")";
    }

    public String getOmimId() {
        return omimId;
    }

    public String getOmimLabel() {
        return omimLabel;
    }

    public List<MdDiseaseTableCell> getCells() {
        return cells;
    }
}
