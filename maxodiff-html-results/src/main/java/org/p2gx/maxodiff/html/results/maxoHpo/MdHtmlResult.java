package org.p2gx.maxodiff.html.results.maxoHpo;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
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



    public MdHtmlResult(
            RankedMaxoResult result,
            int idx,
            int nDiseases,
            int nRepetitions,
            HTMLFrequencyMap htmlFrequencyMap) {
        this.index = idx;
        this.maxoId = result.maxoTerm().termId();
        this.maxoLabel = result.maxoTerm().termLabel();
        this.rankedMaxoResult = result;

        List<HpoFrequency> hpoFrequenciesMica = new ArrayList<>();
        for (HpoFrequency hpoFrequency : result.frequencies()) {
            String omimId = hpoFrequency.omimId();
            String hpoId = hpoFrequency.hpoId();
            float frequency = hpoFrequency.frequency();
            float mica = htmlFrequencyMap.micaForDisease(TermId.of(hpoId), TermId.of(omimId));
            hpoFrequenciesMica.add(new HpoFrequency(omimId, hpoId, frequency, mica));
        }

        repetitionRow = MdRepetitionRow.buildRepetitionRow(nRepetitions, result);

        this.resultRows = MdResultRow.createMaxoResultRows(result, nDiseases, hpoFrequenciesMica, htmlFrequencyMap);
    }

    public List<RepetitionCell> getRepetitionCells() {
        return repetitionRow.getCells();
    }

    public List<MdResultRow> getResultRows() {
        return resultRows;
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
    /** The expected change in rank score if the indicated MAxO investigation is applied */
    public String deltaScore() {
        return String.format("%.1f",this.rankedMaxoResult.maxoScore());
    }

    public double nDiseases() {
        return this.rankedMaxoResult.rankedOmimTermList().size();
    }

    public int nObservedHpoTerms() {
        return this.rankedMaxoResult.hpoTermIds().size();
    }

    public List<SimpleTerm> getHpoHeaders() {
        return this.rankedMaxoResult.hpoTermIds().stream().map(CountedHpoTerm::hpoTerm).collect(Collectors.toList());
    }

    public List<RankedOmimTerm> getRankedOmimTermList() {
        return this.rankedMaxoResult.rankedOmimTermList().stream().toList();
    }


    Map<TermId, Double> termToMicaMap(TermId hpoId, HpoDiseases diseases ) {
        return null;
    }

}
