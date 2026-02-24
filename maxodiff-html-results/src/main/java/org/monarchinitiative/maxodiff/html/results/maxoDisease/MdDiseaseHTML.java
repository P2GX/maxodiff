package org.monarchinitiative.maxodiff.html.results.maxoDisease;

import org.monarchinitiative.maxodiff.core.analysis.RankedMaxoResult;

import java.util.ArrayList;
import java.util.List;

public class MdDiseaseHTML {

    List<MdDiseaseRow> resultsRows;
    List<MdDiseaseColumnHeaderCell> maxoHeaders;

    public MdDiseaseHTML(List<RankedMaxoResult> results) {
        this.maxoHeaders = new ArrayList<>();
        results.forEach(result -> maxoHeaders.add(new MdDiseaseColumnHeaderCell(result)));

        this.resultsRows= MdDiseaseRow.createMaxoDiseaseRows(results);
    }

    public List<MdDiseaseRow> getResultsRows() { return this.resultsRows;}

    public List<MdDiseaseColumnHeaderCell> getMaxoHeaders() { return this.maxoHeaders;}

}
