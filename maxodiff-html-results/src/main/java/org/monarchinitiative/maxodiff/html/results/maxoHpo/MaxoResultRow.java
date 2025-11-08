package org.monarchinitiative.maxodiff.html.results.maxoHpo;

import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class MaxoResultRow {
    private final String omimId;
    private final String omimLabel;
    private final int initialRank;
    private final int rankChange;
    private final int nDiseases;
    private final List<HpoTableCell> cells;

    public MaxoResultRow(String omimId, String omimLabel, int initialRank, int rankChange, int nDiseases, List<HpoTableCell> cells) {
        this.omimId = omimId;
        this.omimLabel = omimLabel;
        this.initialRank = initialRank;
        this.rankChange = rankChange;
        this.nDiseases = nDiseases;
        this.cells = cells;
    }


    public static List<MaxoResultRow> createMaxoResultRows(MaxodiffResult result,
                                                           Map<TermId, String> omimTermMap,
                                                           int nDiseases,
                                                           List<TermId> orderedDiscoverableHpoList,
                                                           List<HpoFrequency> hpoFrequenciesMica) {
        List<MaxoResultRow> rows = new ArrayList<>();
        var hpoTermIdRepCtsMap = result.rankMaxoScore().hpoTermIdRepCtsMap();
        Map<TermId, List<Integer>> avgRankChangeMap = result.rankMaxoScore().maxoDiseaseAvgRankChangeMap();
        if (avgRankChangeMap == null) {
            System.err.println("avgRankChangeMap==null");
            return rows;
        }

        for (TermId omimId : result.rankMaxoScore().maxoDiseaseAvgRankChangeMap().keySet()) {
            String omimLabel = omimTermMap.get(omimId);
            int initialRank = Optional.ofNullable(avgRankChangeMap.get(omimId).getFirst())
                    .orElse(0);
            int rankChange = Optional.ofNullable(avgRankChangeMap.get(omimId).getLast())
                    .orElse(0); // TODO -- ARE WE MISSING SOME VALUES WE NEED? CRASH WITHOUT THIS LINE
            List<HpoTableCell> cells = new ArrayList<>();
            for (TermId hpoId : orderedDiscoverableHpoList) {
                Map<TermId, Integer> ctMap = hpoTermIdRepCtsMap.get(omimId);
                if (ctMap == null) {
                    ctMap = new HashMap<>();
                }

                int ct1 = ctMap.getOrDefault(hpoId, 0);
                float mica = 0f;
                Optional<HpoFrequency> hpoFrequencyOpt = hpoFrequenciesMica.stream()
                    .filter(hpoFrequency ->
                        (hpoFrequency.omimId().equals(omimId.toString()) && hpoFrequency.hpoId().equals(hpoId.toString())))
                    .findFirst();
                if (hpoFrequencyOpt.isPresent()) {
                    mica = hpoFrequencyOpt.get().mica();
                }
                float maxMica = 8.343077871169383f;
                double opacity1 = mica / maxMica;
                cells.add(new HpoTableCell(ct1, opacity1, mica));
            }
            rows.add(new MaxoResultRow(omimId.getValue(), omimLabel, initialRank, rankChange, nDiseases, cells));

        }
        return rows;
    }

    public String getStyle() {
        double opacity = (double) rankChange / nDiseases;
        return (rankChange < 0) ? "rgba(0, 128, 0, " + (-1.0 * opacity) + ")" :
                "rgba(255, 0, 0, " + opacity + ")";
    }

    public String getOmimId() {
        return omimId;
    }

    public String getOmimIdDigits() {
        String[] fields =  omimId.split(":");
        return fields.length == 2 ? fields[1] : omimId;
    }

    public String getOmimLabel() {
        return omimLabel;
    }

    public int getInitialRank() {
        return initialRank;
    }

    public int getRankChange() {
        return rankChange;
    }

    public int getnDiseases() {
        return nDiseases;
    }

    public List<HpoTableCell> getCells() {
        return cells;
    }
}
