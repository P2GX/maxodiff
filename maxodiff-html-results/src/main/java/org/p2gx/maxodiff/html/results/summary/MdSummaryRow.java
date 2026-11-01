package org.p2gx.maxodiff.html.results.summary;

import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;
import org.p2gx.maxodiff.core.analysis.RankedOmimTerm;

import java.util.Locale;

/**
 * One row of the summary table for a MAxO term that is not shown in full detail.
 *
 * @param position   the position of the term in the list of all terms, best first
 * @param nImproved  number of diseases that move up the list on average
 * @param nDeclined  number of diseases that move down the list on average
 */
public record MdSummaryRow(int position, String maxoId, String maxoLabel, String deltaScore,
                           int nHpoTerms, int nImproved, int nDeclined) {

    public static MdSummaryRow of(int position, RankedMaxoResult result) {
        int improved = 0;
        int declined = 0;
        for (RankedOmimTerm disease : result.rankedOmimTermList()) {
            float change = disease.signedRankChange();   // negative = moves up the list
            if (change < 0) {
                improved++;
            } else if (change > 0) {
                declined++;
            }
        }
        return new MdSummaryRow(position,
                result.maxoTerm().tid().getValue(),
                result.maxoTerm().label(),
                String.format(Locale.ROOT, "%.1f", result.maxoScore()),
                result.hpoTermIds().size(),
                improved,
                declined);
    }
}