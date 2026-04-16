package org.p2gx.maxodiff.html.results.maxoHpo;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.CountedHpoTerm;
import org.p2gx.maxodiff.core.analysis.HpoFrequency;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;
import org.p2gx.maxodiff.core.analysis.RankedOmimTerm;

import java.util.ArrayList;
import java.util.List;

/** Compute data for the N Repetitions row */
public class MdRepetitionRow {
    private final List<RepetitionCell> cells;

    public MdRepetitionRow(List<RepetitionCell> cells) {
        this.cells = cells;
    }


    /** In current implementation, a null count is a signal that the disease in question is not annotated to the
     * HPO term. Therefore, we do not count it.
     * @param counts
     * @return
     */
    private static String getCountsString(Integer counts) {
        return (counts == null) ? "" : counts.toString();
    }

    private static String getStyleString(Integer counts, int nRepetitions) {
        double opacity =  (counts * 1.0) / nRepetitions;
        return "rgba(255, 215, 0, " + opacity + ")";
    }

    private static List<RepetitionCellTooltipItem> getTooltipItems(List<HpoFrequency> freqs,
                                                                   List<RankedOmimTerm> omimTerms) {
        List<RepetitionCellTooltipItem> items = new ArrayList<>();

        for (HpoFrequency freq : freqs) {
            float frequency = freq.frequency();
            String percentage = String.format("%.1f%%", frequency * 100);
            String omimLabel = omimTerms.stream()
                    .filter(ro -> ro.omimTerm().tid().equals(freq.diseaseId()))
                    .findFirst().get().omimTerm().label();
            String name = String.join("; ", omimLabel);
            items.add(new RepetitionCellTooltipItem(name, percentage));
        }
        return items;
    }



    public static MdRepetitionRow buildRepetitionRow(int nRepetitions, RankedMaxoResult result) {
        List<RepetitionCell> cells = new ArrayList<>();

        for (CountedHpoTerm ctHpoTerm : result.hpoTermIds()) {
            Integer ct = ctHpoTerm.count();
            String ctString = MdRepetitionRow.getCountsString(ct);
            String styleString = MdRepetitionRow.getStyleString(ct, nRepetitions);
            TermId hpoId = ctHpoTerm.hpoTerm().tid();
            String hpoLabel = ctHpoTerm.hpoTerm().label();
            List<HpoFrequency> freqs = result.frequencies().stream()
                    .filter(f -> f.hpoId().equals(hpoId)).toList();
            List<RepetitionCellTooltipItem> tooltipitems = MdRepetitionRow.getTooltipItems(freqs,
                    result.rankedOmimTermList());
            RepetitionCell cell = new RepetitionCell(ctString, styleString, hpoLabel, tooltipitems);
            cells.add(cell);
        }

        return new MdRepetitionRow(cells);
    }

    public List<RepetitionCell> getCells() {
        return cells;
    }
}
