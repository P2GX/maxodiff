package org.monarchinitiative.maxodiff.html.results.maxoHpo;

import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class MaxoResultRow {
    private final String omimId;
    private final String omimLabel;
    private final int rankChange;
    private final int nDiseases;
    private final List<HpoTableCell> cells;

    public MaxoResultRow(String omimId, String omimLabel, int rankChange, int nDiseases, List<HpoTableCell> cells) {
        this.omimId = omimId;
        this.omimLabel = omimLabel;
        this.rankChange = rankChange;
        this.nDiseases = nDiseases;
        this.cells = cells;
    }


    public static List<MaxoResultRow> createMaxoResultRows(MaxodiffResult result,
                                                           Map<TermId, String> omimTermMap,
                                                           int nDiseases,
                                                           List<TermId> orderedDiscoverableHpoList) {
        List<MaxoResultRow> rows = new ArrayList<>();
        var hpoTermIdRepCtsMap = result.rankMaxoScore().hpoTermIdRepCtsMap();
        Map<TermId, Integer> nRepetitionsMap = new HashMap<>();
        for (Map.Entry<TermId, Map<TermId, Integer>> diseaseHpoRepCtEntry : hpoTermIdRepCtsMap.entrySet()) {
            Map<TermId, Integer> hpoRetCtMap = diseaseHpoRepCtEntry.getValue();
            for (Map.Entry<TermId, Integer> hpoRepCtMapEntry : hpoRetCtMap.entrySet()) {
                TermId hpoId = hpoRepCtMapEntry.getKey();
                Integer repCt = hpoRepCtMapEntry.getValue();
                if (repCt != null && !nRepetitionsMap.containsKey(hpoId)) {
                    nRepetitionsMap.put(hpoId, repCt);
                    break;
                }
            }
        }
        Map<TermId, Integer> avgRankChangeMap = result.rankMaxoScore().maxoDiseaseAvgRankChangeMap();
        if (avgRankChangeMap == null) {
            System.err.println("avgRankChangeMap==null");
            return rows;
        }

        for (TermId omimId : result.rankMaxoScore().maxoDiseaseAvgRankChangeMap().keySet()) {
            String omimLabel = omimTermMap.get(omimId);
            int rankChange = Optional.ofNullable(avgRankChangeMap.get(omimId))
                    .orElse(0); // TODO -- ARE WE MISSING SOME VALUES WE NEED? CRASH WITHOUT THIS LINE
            List<HpoTableCell> cells = new ArrayList<>();
            for (TermId hpoId : orderedDiscoverableHpoList) {
                Map<TermId, Integer> ctMap = hpoTermIdRepCtsMap.get(omimId);
                if (ctMap == null) {
                    ctMap = new HashMap<>();
                }
                /// TODO WHAT?
                int ct1 = Optional.ofNullable(ctMap.get(hpoId)).orElse(0);
                double opacity1 =
                        (result.rankMaxoScore().discoverableObservedDescendantHpoTermIds().contains(hpoId) ? 0.5 : 1);
                cells.add(new HpoTableCell(ct1, opacity1));
            }
            rows.add(new MaxoResultRow(omimId.getValue(), omimLabel, rankChange, nDiseases, cells));

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

    public String getOmimLabel() {
        return omimLabel;
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
