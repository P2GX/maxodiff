package org.monarchinitiative.maxodiff.html.results.maxoDisease;

import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaxoDiseaseHTML {

    List<MaxoDiseaseRow> resultsRows;
    List<MaxoDiseaseColumnHeaderCell> maxoHeaders;

    public MaxoDiseaseHTML(List<MaxodiffResult> results, BiometadataService biometadataService) {
        this.maxoHeaders = new ArrayList<>();
        results.forEach(result -> maxoHeaders.add(new MaxoDiseaseColumnHeaderCell(result, biometadataService)));

        Map<TermId, String> omimTermMap = new HashMap<>();
        results.getFirst().rankMaxoScore().initialOmimTermIds()
                .forEach(id -> omimTermMap.put(id, biometadataService.diseaseLabel(id).orElse("unknown")));
        this.resultsRows= MaxoDiseaseRow.createMaxoDiseaseRows(results, omimTermMap, biometadataService);
    }

    public List<MaxoDiseaseRow> getResultsRows() { return this.resultsRows;}

    public List<MaxoDiseaseColumnHeaderCell> getMaxoHeaders() { return this.maxoHeaders;}

}
