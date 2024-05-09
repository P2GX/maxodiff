package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.io.MaxoDxAnnots;
import org.monarchinitiative.maxodiff.core.io.MaxodiffDataException;
import org.monarchinitiative.maxodiff.core.io.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.core.io.PhenopacketFileParser;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.maxodiff.core.analysis.DiseaseTermCountImpl.HpoFrequency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

import static org.monarchinitiative.maxodiff.core.io.MaxodiffBuilder.loadOntology;

public class MaxoTermScoreMap {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaxoTermScoreMap.class);

    public record MaxoTermScore(String maxoTerm, String maxoLabel, Integer nOmimTerms, Set<TermId> omimTermIds,
                                Integer nHpoTerms, Set<SimpleTerm> hpoTerms, Double score) {}

    public record Frequencies(TermId hpoId, String hpoLabel, List<Float> frequencies) {}

    MaxodiffDataResolver dataResolver;
    Ontology hpo;
    MaxoDxAnnots maxoDxAnnots;
    Map<SimpleTerm, Set<SimpleTerm>> fullHpoToMaxoTermMap;

    List<HpoDisease> diseases;
    Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap;

    TermId diseaseId;
    Map<TermId, List<HpoFrequency>> hpoTermCounts;

    public MaxoTermScoreMap(Path maxoDataPath) throws MaxodiffDataException {
        this.dataResolver = new MaxodiffDataResolver(maxoDataPath);
        this.hpo = loadOntology(dataResolver.hpoJson());
        this.maxoDxAnnots = new MaxoDxAnnots(dataResolver.maxoDxAnnots());
        this.fullHpoToMaxoTermMap = maxoDxAnnots.getSimpleTermSetMap();
    }


    public List<MaxoTermScore> getMaxoTermRecords(Path phenopacketPath, List<LiricalResultsFileRecord> liricalOutputRecords,
                                                  double posttestFilter, double weight) throws Exception {
        List<MaxoTermScore> maxoTermScoreRecords = new ArrayList<>();
        Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoTermMap = makeMaxoToHpoTermMap(liricalOutputRecords, phenopacketPath, posttestFilter);
        Map<SimpleTerm, Double> maxoScoreMap = makeMaxoScoreMap(maxoToHpoTermMap, liricalOutputRecords, weight);
        LOGGER.info(maxoScoreMap.toString());
        maxoToHpoTermMap.forEach((key, value) -> {
            String maxoId = key.tid().toString();
            String maxoTermLabel = key.label();
            int nOmimTerms = diseases.size();
            Set<TermId> omimIds = new HashSet<>();
            diseases.forEach(disease -> omimIds.add(disease.id()));
            Set<SimpleTerm> hpoTerms = value;
            Integer nHpoTerms = value.size();
            double score = maxoScoreMap.get(key);
            maxoTermScoreRecords.add(new MaxoTermScore(maxoId, maxoTermLabel, nOmimTerms, omimIds, nHpoTerms, hpoTerms, score));
            Comparator<MaxoTermScore> comp = Comparator.comparing(MaxoTermScore::score, Comparator.reverseOrder());
            maxoTermScoreRecords.sort(comp);
        });
        return maxoTermScoreRecords;
    }

    public List<Frequencies> getFrequencyRecords(MaxoTermScore maxoTermScoreRecord) {
        List<Frequencies> frequencyRecords = new ArrayList<>();
        Set<TermId> omimIds = maxoTermScoreRecord.omimTermIds();
        for (SimpleTerm hpoTerm : maxoTermScoreRecord.hpoTerms) {
            TermId hpoId = hpoTerm.tid();
            String hpoLabel = hpoTerm.label();
            Map<TermId, Float> maxoFrequencies = new LinkedHashMap<>();
            omimIds.forEach(e -> maxoFrequencies.put(e, null));
            List<HpoFrequency> frequencies = hpoTermCounts.get(hpoId);
            for (HpoFrequency hpoFrequency : frequencies) {
                for (TermId omimId : omimIds) {
                    if (hpoFrequency.omimId().equals(omimId.toString())) {
                        Float frequency = hpoFrequency.frequency();
                        maxoFrequencies.replace(omimId, frequency);
                    }
                }
            }
            frequencyRecords.add(new Frequencies(hpoId, hpoLabel, maxoFrequencies.values().stream().toList()));
        }
        return frequencyRecords;
    }

    public Map<SimpleTerm, Set<SimpleTerm>> makeMaxoToHpoTermMap(List<LiricalResultsFileRecord> liricalOutputRecords,
                                                             Path phenopacketPath, double posttestFilter) throws Exception {

        PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(phenopacketPath);
        diseaseId = phenopacketData.diseaseIds().get(0);
        LOGGER.info("Min Posttest Probabiltiy Threshold = " + posttestFilter);
        // Collect HPO terms and frequencies for the target m diseases
        List<TermId> diseaseIds = new ArrayList<>();
        if (liricalOutputRecords != null) {
            List<LiricalResultsFileRecord> orderedResults = liricalOutputRecords.stream()
                    .sorted(Comparator.comparingDouble(LiricalResultsFileRecord::posttestProbability).reversed()).toList();
            List<LiricalResultsFileRecord> records = orderedResults.stream()
                    .filter(r -> r.posttestProbability() >= posttestFilter)
                    .toList();
            records.forEach(r -> diseaseIds.add(r.omimId()));
        }
        LOGGER.info(phenopacketPath + " diseaseIds: " + String.valueOf(diseaseIds));
        int topNDiseases = diseaseIds.size();

        diseases = DifferentialDiagnosis.makeDiseaseList(dataResolver, diseaseIds);
        DiseaseTermCount diseaseTermCount = DiseaseTermCount.of(diseases);
        hpoTermCounts = diseaseTermCount.hpoTermCounts();

        // Remove HPO terms present in the phenopacket
        phenopacketData.presentHpoTermIds().forEach(hpoTermCounts::remove);
        phenopacketData.excludedHpoTermIds().forEach(hpoTermCounts::remove);

        // Get all the MaXo terms that can be used to diagnose the HPO terms
        hpoToMaxoTermMap = DifferentialDiagnosis.makeHpoToMaxoTermMap(fullHpoToMaxoTermMap, hpoTermCounts.keySet());
        Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoTermMap = DifferentialDiagnosis.makeMaxoToHpoTermMap(hpo, hpoToMaxoTermMap);

        LOGGER.info(maxoToHpoTermMap.toString());

        return maxoToHpoTermMap;
    }

    public Map<SimpleTerm, Double> makeMaxoScoreMap(Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoTermMap,
                                                List<LiricalResultsFileRecord> liricalOutputRecords, Double weight) {
        return DifferentialDiagnosis.makeMaxoScoreMap(maxoToHpoTermMap, diseases, liricalOutputRecords, weight);
    }

    public TermId getDiseaseId() {
        return diseaseId;
    }

    public List<HpoDisease> getDiseases() {
        return diseases;
    }

    public Map<SimpleTerm, Set<SimpleTerm>> getHpoToMaxoTermMap() {
        return hpoToMaxoTermMap;
    }

}
