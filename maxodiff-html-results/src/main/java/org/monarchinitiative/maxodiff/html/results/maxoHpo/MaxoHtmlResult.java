package org.monarchinitiative.maxodiff.html.results.maxoHpo;

import org.monarchinitiative.maxodiff.core.analysis.HTMLFrequencyMap;
import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.RankMaxoScore;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.html.results.HtmlResults;
import org.monarchinitiative.maxodiff.html.results.SimpleTerm;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;

import java.util.*;

/**
 * This is a class that will be used to hold data for each MAxO result
 * that we show in the HTML.
 */
public class MaxoHtmlResult {
    private final int index;
    private final String maxoId;
    private final String maxoLabel;
    private RankMaxoScore rankMaxoScore;
    private final Map<TermId, String> hpoTermsMap;
    private final Map<TermId, String> omimTerms;
    private Map<String, Map<Float, List<String>>> frequencyMap;

    private Map<TermId, Map<TermId, Integer>> hpoTermIdRepCtsMap;
    private int nDiscoverableHpo;
    private final MaxodiffResult maxodiffResult;
    private final RepetitionRow repetitionRow;
    private final List<MaxoResultRow> resultRows;
    private final List<TermId> orderedDiscoverableHpoList;



    public MaxoHtmlResult(
            MaxodiffResult result,
            Map<TermId, List<HpoFrequency>> hpoTermCountMap,
            int idx,
            int nDiseases,
            int nRepetitions,
            BiometadataService biometadataService,
            HTMLFrequencyMap htmlFrequencyMap) {
        this.hpoTermsMap = new HashMap<>();
        this.omimTerms = new HashMap<>();
        Map<TermId, Integer> nRepetitionsMap = new HashMap<>();
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
        List<HpoFrequency> hpoFrequenciesMica = new ArrayList<>();
        for (TermId omimId : result.rankMaxoScore().maxoOmimTermIds()) {
            for (TermId hpoId : result.rankMaxoScore().discoverableObservedHpoTermIds()) {
                int count = 0;
                float frequency = 0f;
                if (hpoTermCountMap.get(omimId) != null) {
                    Optional<HpoFrequency> hpoFrequencyOpt = hpoTermCountMap.get(omimId).stream()
                            .filter(hpoFrequency -> hpoFrequency.hpoId().equals(hpoId.toString())).findFirst();
                    if (hpoFrequencyOpt.isPresent()) {
                        count = hpoFrequencyOpt.get().count();
                        frequency = hpoFrequencyOpt.get().frequency();
                    }
                }
                float mica = htmlFrequencyMap.micaForDisease(hpoId, omimId);
                hpoFrequenciesMica.add(new HpoFrequency(omimId.toString(), hpoId.toString(), count, frequency, mica));
            }
        }
        this.hpoTermIdRepCtsMap = result.rankMaxoScore().hpoTermIdRepCtsMap();
        this.frequencyMap = HTMLFrequencyMap.makeFrequencyDiseaseMap(hpoTermsMap,
                omimTerms,
                hpoTermIdRepCtsMap,
                hpoFrequencies);
        Set<TermId> discoverableTermIdSet = result.rankMaxoScore().discoverableObservedHpoTermIds();
        this.orderedDiscoverableHpoList = new ArrayList<>(discoverableTermIdSet);
        repetitionRow = RepetitionRow.buildRepetitionRow(
                nRepetitionsMap, nRepetitions, this.hpoTermsMap, frequencyMap, this.orderedDiscoverableHpoList, result);

        this.resultRows = MaxoResultRow.createMaxoResultRows(result, omimTerms, nDiseases, this.orderedDiscoverableHpoList, hpoFrequenciesMica);
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

    public List<SimpleTerm> getHpoHeaders() {
        List<SimpleTerm> hpoTerms = new ArrayList<>();
        for (TermId termId : orderedDiscoverableHpoList) {
            String label = hpoTermsMap.getOrDefault(termId, "N/A");
            hpoTerms.add(new SimpleTerm(termId.getValue(), label));
        }
        return hpoTerms;
    }


    Map<TermId, Double> termToMicaMap(TermId hpoId, HpoDiseases diseases ) {
        return null;
    }

}
