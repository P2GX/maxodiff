package org.monarchinitiative.maxodiff.html.results.maxoDisease;

import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class MaxoDiseaseRow {
    private final String omimId;
    private final String omimLabel;
    private final Double score;
    private final Double topScore;
    private final List<MaxoDiseaseTableCell> cells;

    public MaxoDiseaseRow(String omimId, String omimLabel, Double score, Double topScore, List<MaxoDiseaseTableCell> cells) {
        this.omimId = omimId;
        this.omimLabel = omimLabel;
        this.score = score;
        this.topScore = topScore;
        this.cells = cells;
    }


    public static List<MaxoDiseaseRow> createMaxoDiseaseRows(List<MaxodiffResult> results,
                                                             Map<TermId, String> omimTermMap) {

        List<MaxoDiseaseRow> rows = new ArrayList<>();
        for (TermId omimId : results.getFirst().rankMaxoScore().initialOmimTermIds()) {
            String omimLabel = omimTermMap.get(omimId);
            List<MaxoDiseaseTableCell> cells = new ArrayList<>();
            double score = 0.;
            double topScore = results.getFirst().rankMaxoScore().maxoScore();
            for (MaxodiffResult result : results) {
                Optional<TermId> firstMaxoDiseaseIdOpt = result.rankMaxoScore().maxoDiseaseAvgRankChangeMap().keySet().stream().findFirst();
                if (firstMaxoDiseaseIdOpt.isPresent()) {
                    TermId firstMaxoDiseaseId = firstMaxoDiseaseIdOpt.get();
                    if (firstMaxoDiseaseId == omimId) {
                        score = result.rankMaxoScore().maxoScore();
                    } else {
                        score = 0.;
                    }
                }
                double opacity = score / topScore;
                cells.add(new MaxoDiseaseTableCell(score, opacity));
            }
            rows.add(new MaxoDiseaseRow(omimId.getValue(), omimLabel, score, topScore, cells));

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

    public List<MaxoDiseaseTableCell> getCells() {
        return cells;
    }
}
