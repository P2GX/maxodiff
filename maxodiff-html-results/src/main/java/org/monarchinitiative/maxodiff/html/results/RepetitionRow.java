package org.monarchinitiative.maxodiff.html.results;

import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

/** Compute data for the N Repetitions row */
public class RepetitionRow {
    private final List<RepetitionCell> cells;

    public RepetitionRow(List<RepetitionCell> cells) {
        this.cells = cells;
    }





    private static String getCountsString(Integer counts) {
        return (counts == null) ? "na" : counts.toString();
    }

    private static String getStyleString(Integer counts, int nRepetitions) {
        double opacity = (counts == null) ? 0 : (counts * 1.0) / nRepetitions;
        opacity = 0.5;
        return "rgba(255, 215, 0, " + opacity + ")";
    }

    private static String getTooltip(TermId hpoId, String hpoLabel, Map<Float,List<String>> freqMap) {
        StringBuilder freqHTML = new StringBuilder();
        freqHTML.append("<ul>");
        for (var entry : freqMap.entrySet()) {
            Float frequency = entry.getKey();
            String percentage = String.format("%.1f%%", frequency * 100);
            String omimLabelString = String.join("; ", entry.getValue());
            freqHTML.append("<li><span>")
                        .append(omimLabelString)
                        .append("</span>: ")
                        .append(percentage)
                        .append("</li>");
        }
        freqHTML.append("</ul>");

        return "<div style='background-color: lightgray; color: red'><b>HPO Term</b>: " + hpoLabel + "</div>" +
                "<div><p></p></div>" +
                freqHTML;
    }



    public static  RepetitionRow buildRepetitionRow(
            Map<TermId,Integer> nRepetitionsMap,
            int nRepetitions,
            Map<TermId,String> hpoTermsMap,
            Map<String, Map<Float, List<String>>> frequencyMap,
            List<TermId> hpoIds,
            MaxodiffResult result) {
        List<RepetitionCell> cells = new ArrayList<>();
        var hpoTermIdRepCtsMap = result.rankMaxoScore().hpoTermIdRepCtsMap();
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

        for (TermId hpoId : hpoIds) {
            Integer ct = nRepetitionsMap.get(hpoId);
            String ctString = RepetitionRow.getCountsString(ct);
            String styleString = RepetitionRow.getStyleString(ct, nRepetitions);
            String hpoLabel = hpoTermsMap.get(hpoId);;
            Map<Float,List<String>> freqMap = frequencyMap.get(hpoLabel);
            String tooltip = RepetitionRow.getTooltip(hpoId, ctString, freqMap);
            RepetitionCell cell = new RepetitionCell(ctString, styleString, tooltip);
            cells.add(cell);
        }

        return new RepetitionRow(cells);
    }

    public List<RepetitionCell> getCells() {
        return cells;
    }
}
