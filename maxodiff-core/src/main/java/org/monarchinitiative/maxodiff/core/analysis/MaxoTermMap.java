package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.io.MaxoDxAnnots;
import org.monarchinitiative.maxodiff.core.io.MaxodiffDataException;
import org.monarchinitiative.maxodiff.core.io.MaxodiffDataResolver;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.maxodiff.core.analysis.DiseaseTermCountImpl.HpoFrequency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

import static org.monarchinitiative.maxodiff.core.io.MaxodiffBuilder.loadOntology;

public class MaxoTermMap {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaxoTermMap.class);

    public record MaxoTermScore(String maxoId, String maxoLabel, Integer nOmimTerms, Set<TermId> omimTermIds,
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

    public MaxoTermMap(Path maxoDataPath) throws MaxodiffDataException {
        this.dataResolver = new MaxodiffDataResolver(maxoDataPath);
        this.hpo = loadOntology(dataResolver.hpoJson());
        this.maxoDxAnnots = new MaxoDxAnnots(dataResolver.maxoDxAnnots());
        this.fullHpoToMaxoTermMap = maxoDxAnnots.getSimpleTermSetMap();
    }

    public AnalysisResults runLiricalCalculation(LiricalAnalysis liricalAnalysis, Path phenopacketPath) throws Exception {
        return liricalAnalysis.runLiricalAnalysis(phenopacketPath);
    }

    /**
     *
     * @param phenopacketPath
     * @param results {@link AnalysisResults}. LIRICAL analysis results.
     * @param liricalOutputRecords List<LiricalResultsFileRecord>. List of {@link LiricalResultsFileRecord} results from LIRICAL output file.
     * @param posttestFilter double. Posttest probability threshold to get list of disease Ids to use for differential diagnosis calculation.
     * @param weight double. Weight value to use in the differential diagnosis calculation.
     * @return List<MaxoTermScore>. List of {@link MaxoTermScore} records, in order of decreasing score.
     * @throws Exception
     */
    public List<MaxoTermScore> getMaxoTermRecords(Path phenopacketPath, AnalysisResults results, List<LiricalResultsFileRecord> liricalOutputRecords,
                                                  double posttestFilter, double weight) throws Exception {
        List<MaxoTermScore> maxoTermScoreRecords = new ArrayList<>();
        Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoTermMap = makeMaxoToHpoTermMap(results, liricalOutputRecords, phenopacketPath, posttestFilter);
        Map<SimpleTerm, Double> maxoScoreMap = makeMaxoScoreMap(maxoToHpoTermMap, results, liricalOutputRecords, weight);
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

    /**
     *
     * @param maxoTermScoreRecord {@link MaxoTermScore} record.
     * @return List<Frequencies>. List of {@link Frequencies} records.
     */
    public List<Frequencies> getFrequencyRecords(MaxoTermScore maxoTermScoreRecord) {
        List<Frequencies> frequencyRecords = new ArrayList<>();
        Set<TermId> omimIds = maxoTermScoreRecord.omimTermIds();
        for (SimpleTerm hpoTerm : maxoTermScoreRecord.hpoTerms()) {
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

    /**
     *
     * @param results {@link AnalysisResults}. LIRICAL analysis results.
     * @param liricalOutputRecords List<LiricalResultsFileRecord>. List of {@link LiricalResultsFileRecord} results from LIRICAL output file.
     * @param phenopacketPath Path. Path to phenopacket file.
     * @param posttestFilter double. Posttest probability threshold to get list of disease Ids to use for differential diagnosis calculation.
     * @return Map<SimpleTerm, Set<SimpleTerm>>. Map of MaXo terms to Set of associated HPO terms, not including ancestors.
     * @throws Exception
     */
    public Map<SimpleTerm, Set<SimpleTerm>> makeMaxoToHpoTermMap(AnalysisResults results, List<LiricalResultsFileRecord> liricalOutputRecords,
                                                             Path phenopacketPath, double posttestFilter) throws Exception {

        PhenopacketData phenopacketData = LiricalAnalysis.readPhenopacketData(phenopacketPath);
        diseaseId = phenopacketData.diseaseIds().get(0);
        LOGGER.info("Min Posttest Probabiltiy Threshold = " + posttestFilter);
        // Collect HPO terms and frequencies for the target m diseases
        List<TermId> diseaseIds = new ArrayList<>();
        if (results != null) {
            List<TestResult> testResults = results.resultsWithDescendingPostTestProbability()
                    .filter(r -> r.posttestProbability() >= posttestFilter)
                    .toList();
            testResults.forEach(r -> diseaseIds.add(r.diseaseId()));
        } else if (liricalOutputRecords != null) {
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

    /**
     *
     * @param maxoToHpoTermMap Map<SimpleTerm, Set<SimpleTerm>>. Map of MaXo terms to Set of associated HPO terms, not including ancestors.
     * @param results {@link AnalysisResults}. LIRICAL analysis results.
     * @param liricalOutputRecords List<LiricalResultsFileRecord>. List of {@link LiricalResultsFileRecord} results from LIRICAL output file.
     * @param weight double. Weight value to use in the differential diagnosis calculation.
     * @return Map<SimpleTerm, Double>. Map of MaXo terms to final differential diagnosis scores.
     */
    public Map<SimpleTerm, Double> makeMaxoScoreMap(Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoTermMap, AnalysisResults results,
                                                List<LiricalResultsFileRecord> liricalOutputRecords, Double weight) {
        return DifferentialDiagnosis.makeMaxoScoreMap(maxoToHpoTermMap, diseases, results, liricalOutputRecords, weight);
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
