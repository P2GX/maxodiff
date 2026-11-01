package org.p2gx.maxodiff.html.results.maxoHpo;

import org.p2gx.maxodiff.core.analysis.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This is a class that will be used to hold data for each MAxO result
 * that we show in the HTML.
 */
public class MdHtmlResult {
    private final int index;
    private final String maxoId;
    private final String maxoLabel;
    private final RankedMaxoResult rankedMaxoResult;
    private final MdRepetitionRow repetitionRow;
    private final List<MdResultRow> resultRows;
    private final List<ScaleTick> rankScale;


    public MdHtmlResult(
            RankedMaxoResult result,
            int idx,
            int nDiseases,
            int nRepetitions,
            HTMLFrequencyMap htmlFrequencyMap,
            double maxMica) {
        this.index = idx;
        this.maxoId = result.maxoTerm().tid().getValue();
        this.maxoLabel = result.maxoTerm().label();
        this.rankedMaxoResult = result;
        this.rankScale = RankScale.ticks(nDiseases);
        repetitionRow = MdRepetitionRow.buildRepetitionRow(nRepetitions, result);

        this.resultRows = MdResultRow.createMaxoResultRows(result, nDiseases, htmlFrequencyMap, maxMica);
    }

    public int index() {
        return this.index;
    }

    public String maxoId() {
        return maxoId;
    }

    public String maxoLabel() {
        return maxoLabel;
    }

    public List<ScaleTick> getRankScale() {
        return rankScale;
    }
    /** The expected change in rank score if the indicated MAxO investigation is applied */
    public String deltaScore() {
        return String.format(Locale.ROOT, "%.1f",this.rankedMaxoResult.maxoScore());
    }

    public int nDiseases() {
        return this.rankedMaxoResult.rankedOmimTermList().size();
    }

    public int nObservedHpoTerms() {
        return this.rankedMaxoResult.hpoTermIds().size();
    }

    public List<SimpleTerm> getHpoHeaders() {
        return this.rankedMaxoResult.hpoTermIds().stream().map(CountedHpoTerm::hpoTerm).collect(Collectors.toList());
    }

    public List<MdResultRow> getResultRows() {
        return this.resultRows;
    }

    public MdRepetitionRow getRepetitionRow() {
        return repetitionRow;
    }

    public List<RepetitionCell> getRepetitionCells() {
        return repetitionRow.getCells();
    }




}
