package org.p2gx.maxodiff.html.results.maxoDisease;

import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;
import org.p2gx.maxodiff.core.analysis.RankedOmimTerm;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The overview matrix: one row per test, one column per disease. A cell tells how far the disease moves
 * in the ranking when the test is done (negative: up the list, positive: down).
 */
public final class MdMatrix {

    /** Longest disease name in a column header; the full name stays in the tooltip. */
    static final int MAX_LABEL_LENGTH = 25;
    /** Changes below this many rank positions are shown as "no change". */
    static final float NO_CHANGE = 0.05f;
    /** The same colours as the bars of the tornado plot. */
    private static final String UP = "0,143,251";
    private static final String DOWN = "255,69,96";
    private static final String MINUS = "\u2212";

    private static final String ELLIPSIS = "…";

    /** A disease; the columns follow the order of the initial differential diagnosis. */
    public record Column(String diseaseId, String label, String shortLabel, int initialRank, String url) { }

    /** One cell. {@code text} and {@code style} are empty if the disease does not move. */
    public record Cell(String text, String style, String title) {
        public boolean isEmpty() {
            return text.isEmpty();
        }
    }

    public record Row(int position, String maxoId, String maxoLabel, List<Cell> cells) { }

    private final List<Column> columns;
    private final List<Row> rows;

    private MdMatrix(List<Column> columns, List<Row> rows) {
        this.columns = columns;
        this.rows = rows;
    }

    /** @param results the tests to list, best first; they all rank the same diseases */
    public static MdMatrix of(List<RankedMaxoResult> results) {
        if (results.isEmpty()) {
            return new MdMatrix(List.of(), List.of());
        }
        List<Column> columns = results.getFirst().rankedOmimTermList().stream()
                .sorted(Comparator.comparingInt(d -> d.initialRank() > 0 ? d.initialRank() : Integer.MAX_VALUE))
                .map(d -> {
                    String id = d.omimTerm().tid().getValue();
                    return new Column(id, d.omimTerm().label(), shorten(d.omimTerm().label(), MAX_LABEL_LENGTH),
                            d.initialRank(), urlOf(id));
                })
                .toList();
        double largest = results.stream()
                .flatMap(r -> r.rankedOmimTermList().stream())
                .mapToDouble(d -> Math.abs(d.signedRankChange()))
                .max().orElse(0.0);

        List<Row> rows = new java.util.ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            RankedMaxoResult result = results.get(i);
            Map<String, RankedOmimTerm> byId = new HashMap<>();   // match by disease id, never by position
            result.rankedOmimTermList().forEach(d -> byId.put(d.omimTerm().tid().getValue(), d));
            String maxoLabel = result.maxoTerm().label();
            List<Cell> cells = columns.stream()
                    .map(c -> cellFor(byId.get(c.diseaseId()), maxoLabel, c.label(), largest))
                    .toList();
            rows.add(new Row(i + 1, result.maxoTerm().tid().getValue(), maxoLabel, cells));
        }
        return new MdMatrix(columns, List.copyOf(rows));
    }

    private static Cell cellFor(RankedOmimTerm disease, String maxoLabel, String diseaseLabel, double largest) {
        String who = maxoLabel + " \u2013 " + diseaseLabel;
        if (disease == null) {
            return new Cell("", "", who + ": not ranked");
        }
        float change = disease.signedRankChange();
        if (Math.abs(change) < NO_CHANGE) {
            return new Cell("", "", who + ": no change");
        }
        double alpha = 0.15 + 0.75 * Math.abs(change) / largest;
        String style = String.format(Locale.ROOT, "background:rgba(%s,%.2f)", change < 0 ? UP : DOWN, alpha);
        String text = (change < 0 ? MINUS : "+") + String.format(Locale.ROOT, "%.1f", Math.abs(change));
        String title = String.format(Locale.ROOT, "%s: %s by %.1f (rank %d \u2192 %.1f)", who,
                change < 0 ? "moves up" : "moves down", Math.abs(change), disease.initialRank(),
                disease.averageRank());
        return new Cell(text, style, title);
    }


    public List<Column> getColumns() {
        return columns;
    }

    public List<Row> getRows() {
        return rows;
    }

    /** @return the text, or its first characters and "..." so that the result is {@code max} long, if it is longer */
    static String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - ELLIPSIS.length()) + ELLIPSIS;
    }

    /** @return the OMIM page of the disease, or null for diseases of other databases */
    static String urlOf(String diseaseId) {
        return diseaseId.startsWith("OMIM:")
                ? "https://www.omim.org/entry/" + diseaseId.substring("OMIM:".length())
                : null;
    }
}