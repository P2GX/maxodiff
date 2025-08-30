package org.monarchinitiative.maxodiff.html.results;

import org.monarchinitiative.maxodiff.core.analysis.Frequencies;
import org.monarchinitiative.maxodiff.core.analysis.HTMLFrequencyMap;
import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.RankMaxoScore;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

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

    private List<HpoPhenotypeData> hpoPhenotypes;
    private List<DiseaseRowData> diseaseRows;
    private final MaxodiffResult maxodiffResult;
    // Static nested classes
    public static class HpoPhenotypeData {
        private final TermId hpoId;
        private final String hpoLabel;
        private final String truncatedLabel;
        private final Integer repetitionCount;
        private final double opacity;
        private final List<Frequencies> frequencies;

        public HpoPhenotypeData(TermId hpoId, String hpoLabel, Integer repetitionCount,
                                int nRepetitions, List<Frequencies> frequencies) {
            this.hpoId = hpoId;
            this.hpoLabel = hpoLabel;
            this.truncatedLabel = hpoLabel.length() > 30 ? hpoLabel.substring(0, 30) + "..." : hpoLabel;
            this.repetitionCount = repetitionCount;
            this.opacity = (repetitionCount == null) ? 0 : (repetitionCount * 1.0) / nRepetitions;
            this.frequencies = frequencies;
        }

        // getters
        public TermId getHpoId() { return hpoId; }
        public String getHpoLabel() { return hpoLabel; }
        public String getTruncatedLabel() { return truncatedLabel; }
        public Integer getRepetitionCount() { return repetitionCount; }
        public double getOpacity() { return opacity; }
        public List<Frequencies> getFrequencies() { return frequencies; }
    }



    public static class DiseaseRowData {
        private final TermId omimId;
        private final String diseaseName;
        private final Integer rankChange;
        private final double opacity;
        private final boolean isImprovement;
        private final List<HpoAssociation> hpoAssociations;

        public DiseaseRowData(TermId omimId, String diseaseName, Integer rankChange,
                              int nDiseases, List<HpoAssociation> hpoAssociations) {
            this.omimId = omimId;
            this.diseaseName = diseaseName;
            this.rankChange = rankChange;
            this.opacity = (rankChange != null) ? Math.abs(rankChange * 1.0 / nDiseases) : 0;
            this.isImprovement = rankChange != null && rankChange < 0;
            this.hpoAssociations = hpoAssociations;
        }

        // getters
        public TermId getOmimId() { return omimId; }
        public String getDiseaseName() { return diseaseName; }
        public Integer getRankChange() { return rankChange; }
        public double getOpacity() { return opacity; }
        public boolean isImprovement() { return isImprovement; }
        public List<HpoAssociation> getHpoAssociations() { return hpoAssociations; }
    }

    public static class HpoAssociation {
        private final TermId hpoId;
        private final double opacity;

        public HpoAssociation(TermId hpoId, Integer count, boolean isDescendant) {
            this.hpoId = hpoId;
            this.opacity = (count != null) ? (isDescendant ? 0.5 : 1.0) : 0;
        }
        public TermId getHpoId() { return hpoId; }
        public double getOpacity() { return opacity; }
    }

    public MaxoHtmlResult(
            MaxodiffResult result,
            List<HpoFrequency> hpoFrequencies,
            int idx,
            BiometadataService biometadataService) {
        this.hpoTermsMap = new HashMap<>();
        this.omimTerms = new HashMap<>();
        this.nRepetitionsMap = new HashMap<>();
        this.frequencyMap = new HashMap<>();
        this.index = idx;
        this.maxoId = result.maxoTermScore().maxoId();
        this.maxoLabel = biometadataService.maxoLabel(maxoId).orElse("unknown");
        this.maxodiffResult = result;
        /*
        result.rankMaxoScore().discoverableObservedHpoTermIds()
                .forEach(id -> hpoTermsMap.put(id, biometadataService.hpoLabel(id).orElse("unknown")));
        result.rankMaxoScore().initialOmimTermIds()
                .forEach(id -> omimTerms.put(id, biometadataService.diseaseLabel(id).orElse("unknown")));
        result.rankMaxoScore().maxoOmimTermIds()
                .forEach(id -> omimTerms.put(id, biometadataService.diseaseLabel(id).orElse("unknown")));
        Map<TermId, Map<TermId, Integer>> hpoTermIdRepCtsMap = result.rankMaxoScore().hpoTermIdRepCtsMap();
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
        Map<String, Map<Float, List<String>>> resultFrequencyMap = HTMLFrequencyMap.makeFrequencyDiseaseMap(hpoTermsMap,
                omimTerms, hpoTermIdRepCtsMap, hpoFrequencies);
        frequencyMap.putAll(resultFrequencyMap);
        this.maxoId = result.maxoTermScore().maxoId();
        this.maxoLabel = biometadataService.maxoLabel(maxoId).orElse("unknown");
        this.maxoTermHeader = idx + ") " + maxoId + ": " + maxoLabel;
        this.nDiseases = result.rankMaxoScore().maxoOmimTermIds().size();
        this.nDiscoverableHpo = result.rankMaxoScore().discoverableObservedHpoTermIds().size();
        this.rankMaxoScore = result.rankMaxoScore();
        this.hpoPhenotypes = processHpoPhenotypes(result);
        this.diseaseRows = processDiseaseRows(result);

         */
    }

    private List<HpoPhenotypeData> processHpoPhenotypes(MaxodiffResult result) {
        List<HpoPhenotypeData> phenotypes = new ArrayList<>();

        for (TermId hpoId : result.rankMaxoScore().discoverableObservedHpoTermIds()) {
            Integer ct = nRepetitionsMap.get(hpoId);
            String hpoLabel = hpoTermsMap.get(hpoId);

            phenotypes.add(new HpoPhenotypeData(hpoId, hpoLabel, ct, nRepetitions, result.frequencies()));
        }

        return phenotypes;
    }

    private List<DiseaseRowData> processDiseaseRows(MaxodiffResult result) {
        List<DiseaseRowData> rows = new ArrayList<>();

        for (TermId omimId : result.rankMaxoScore().maxoDiseaseAvgRankChangeMap().keySet()) {
            String diseaseName = omimTerms.get(omimId);
            Integer rankChange = result.rankMaxoScore().maxoDiseaseAvgRankChangeMap().get(omimId);

            // Process HPO associations for this disease
            List<HpoAssociation> associations = new ArrayList<>();
            for (TermId hpoId : result.rankMaxoScore().discoverableObservedHpoTermIds()) {
                Integer ct = hpoTermIdRepCtsMap.get(omimId).get(hpoId);
                boolean isDescendant = result.rankMaxoScore().discoverableObservedDescendantHpoTermIds().contains(hpoId);
                associations.add(new HpoAssociation(hpoId, ct, isDescendant));
            }

            rows.add(new DiseaseRowData(omimId, diseaseName, rankChange, nDiseases, associations));
        }

        return rows;
    }

    // Add getters for the new fields
    public List<HpoPhenotypeData> getHpoPhenotypes() { return hpoPhenotypes; }
    public List<DiseaseRowData> getDiseaseRows() { return diseaseRows; }



    public int index() {
        return this.index;
    }

    public String maxoId() {
        return maxoId;
    }

    public String maxoLabel() {
        return maxoLabel;
    }

    public double deltaScore() {
        return this.maxodiffResult.rankMaxoScore().maxoScore();
    }

    public double nDiseases() {
        return this.maxodiffResult.rankMaxoScore().maxoOmimTermIds().size();
    }

    public int nObservedHpoTerms() {
        return this.maxodiffResult.rankMaxoScore().discoverableObservedHpoTermIds().size();
    }

}
