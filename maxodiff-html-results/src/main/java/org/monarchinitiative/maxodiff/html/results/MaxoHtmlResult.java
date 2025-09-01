package org.monarchinitiative.maxodiff.html.results;

import org.monarchinitiative.maxodiff.core.analysis.HTMLFrequencyMap;
import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.RankMaxoScore;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This is a class that will be used to hold data for each MAxO result
 * that we show in the HTML.
 */
public class MaxoHtmlResult {
    private int index;
    private String maxoId;
    private String maxoLabel;
    private RankMaxoScore rankMaxoScore;
    private Map<TermId, String> hpoTermsMap;
    private Map<TermId, String> omimTerms;
    private Map<TermId, Integer> nRepetitionsMap;
    private Map<String, Map<Float, List<String>>> frequencyMap;

    private Map<TermId, Map<TermId, Integer>> hpoTermIdRepCtsMap;
    private int nRepetitions;
    private int nDiseases;
    private int nDiscoverableHpo;
    private final MaxodiffResult maxodiffResult;
    private final RepetitionRow repetitionRow;
    private final List<MaxoResultRow> resultRows;
    private final List<TermId> discoverableHpoTermIdList;



    public MaxoHtmlResult(
            MaxodiffResult result,
            Map<TermId, List<HpoFrequency>> hpoTermCountMap,
            int idx,
            BiometadataService biometadataService) {
        this.hpoTermsMap = new HashMap<>();
        this.omimTerms = new HashMap<>();
        this.nRepetitionsMap = new HashMap<>();
        this.index = idx;
        this.maxoId = result.maxoTermScore().maxoId();
        this.maxoLabel = biometadataService.maxoLabel(maxoId).orElse("unknown");
        this.maxodiffResult = result;
        result.rankMaxoScore().discoverableObservedHpoTermIds()
                .forEach(id -> hpoTermsMap.put(id, biometadataService.hpoLabel(id).orElse("unknown")));
        result.rankMaxoScore().initialOmimTermIds()
                .forEach(id -> omimTerms.put(id, biometadataService.diseaseLabel(id).orElse("unknown")));
        result.rankMaxoScore().maxoOmimTermIds()
                .forEach(id -> omimTerms.put(id, biometadataService.diseaseLabel(id).orElse("unknown")));
        List<HpoFrequency> hpoFrequencies = HTMLFrequencyMap.getHpoFrequencies(hpoTermCountMap);
        this.hpoTermIdRepCtsMap = result.rankMaxoScore().hpoTermIdRepCtsMap();
        this.frequencyMap = HTMLFrequencyMap.makeFrequencyDiseaseMap(hpoTermsMap,
                omimTerms,
                hpoTermIdRepCtsMap,
                hpoFrequencies);
        Set<TermId> discoverableTermIdSet = result.rankMaxoScore().discoverableObservedHpoTermIds();
        this.discoverableHpoTermIdList = new ArrayList<>(discoverableTermIdSet);
        repetitionRow = RepetitionRow.buildRepetitionRow(
                 nRepetitionsMap, nRepetitions, this.hpoTermsMap, frequencyMap, discoverableHpoTermIdList ,result);
        this.resultRows = MaxoResultRow.createMaxoResultRows(result, omimTerms, nDiseases);
    }

    public List<RepetitionCell> getRepetitionCells() {
        return repetitionRow.getCells();
    }

    public List<MaxoResultRow> getResultRows() {
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
        return String.format("%.1f",this.maxodiffResult.rankMaxoScore().maxoScore());
    }

    public double nDiseases() {
        return this.maxodiffResult.rankMaxoScore().maxoOmimTermIds().size();
    }

    public int nObservedHpoTerms() {
        return this.maxodiffResult.rankMaxoScore().discoverableObservedHpoTermIds().size();
    }

    public Map<String, String> getHpoHeaders() {
        Map<String, String> hpoHeaders = discoverableHpoTermIdList.stream()
                .collect(Collectors.toMap(
                        TermId::getValue,
                        id -> {
                            String label = hpoTermsMap.getOrDefault(id, "N/A");
                            return label.length() > 30 ? label.substring(0, 30) + "..." : label;
                        }
                ));
        return hpoHeaders;
    }

}
