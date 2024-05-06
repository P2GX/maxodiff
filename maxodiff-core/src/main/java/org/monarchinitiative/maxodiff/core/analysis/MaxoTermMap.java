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
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.maxodiff.core.analysis.DiseaseTermCountImpl.HpoFrequency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;

import static org.monarchinitiative.maxodiff.core.io.MaxodiffBuilder.loadOntology;

public class MaxoTermMap {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaxoTermMap.class);

    public record MaxoTerm(String maxoTerm, String maxoLabel, Integer nOmimTerms, Set<TermId> omimTermIds, Set<SimpleTerm> hpoTerms, Double score) {}

    public record Frequencies(TermId hpoId, String hpoLabel, List<String> frequencies) {}

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

    public List<MaxoTerm> getMaxoTermRecords(Path phenopacketPath, AnalysisResults results, double posttestFilter, double weight) throws Exception {
        List<MaxoTerm> maxoTermRecords = new ArrayList<>();
        Map<TermId, Set<SimpleTerm>> maxoToHpoTermMap = makeMaxoToHpoTermMap(results, phenopacketPath, posttestFilter);
        Map<TermId, Double> maxoScoreMap = makeMaxoScoreMap(maxoToHpoTermMap, results, weight);
        LOGGER.info(maxoScoreMap.toString());
        maxoToHpoTermMap.forEach((key, value) -> {
            String maxoId = key.toString();
            String maxoTermLabel = DifferentialDiagnosis.getMaxoTermLabel(hpoToMaxoTermMap, key);
            int nOmimTerms = diseases.size();
            Set<TermId> omimIds = new HashSet<>();
            diseases.forEach(disease -> omimIds.add(disease.id()));
            double score = maxoScoreMap.get(key);
            maxoTermRecords.add(new MaxoTerm(maxoId, maxoTermLabel, nOmimTerms, omimIds, value, score));
            Comparator<MaxoTerm> comp = Comparator.comparing(MaxoTerm::score, Comparator.reverseOrder());
            maxoTermRecords.sort(comp);
        });
        return maxoTermRecords;
    }

    public List<Frequencies> getFrequencyRecords(MaxoTerm maxoTermRecord) {
        List<Frequencies> frequencyRecords = new ArrayList<>();
        Set<TermId> omimIds = maxoTermRecord.omimTermIds();
        for (SimpleTerm hpoTerm : maxoTermRecord.hpoTerms) {
            TermId hpoId = hpoTerm.tid();
            String hpoLabel = hpoTerm.label();
            Map<TermId, String> maxoFrequencies = new LinkedHashMap<>();
            omimIds.forEach(e -> maxoFrequencies.put(e, "-"));
            List<HpoFrequency> frequencies = hpoTermCounts.get(hpoId);
            for (HpoFrequency hpoFrequency : frequencies) {
                for (TermId omimId : omimIds) {
                    if (hpoFrequency.omimId().equals(omimId.toString())) {
                        Float frequency = hpoFrequency.frequency();
                        maxoFrequencies.replace(omimId, String.format("%.4g%n", frequency));
                    }
                }
            }
            frequencyRecords.add(new Frequencies(hpoId, hpoLabel, maxoFrequencies.values().stream().toList()));
        }
        return frequencyRecords;
    }

    public Map<TermId, Set<SimpleTerm>> makeMaxoToHpoTermMap(AnalysisResults results, Path phenopacketPath, double posttestFilter) throws Exception {

        PhenopacketData phenopacketData = LiricalAnalysis.readPhenopacketData(phenopacketPath);
        diseaseId = phenopacketData.diseaseIds().get(0);
        LOGGER.info("Min Posttest Probabiltiy Threshold = " + posttestFilter);
        // Collect HPO terms and frequencies for the target m diseases
        List<TermId> diseaseIds = new ArrayList<>();
        List<TestResult> testResults = results.resultsWithDescendingPostTestProbability()
                .filter(r -> r.posttestProbability() >= posttestFilter)
                .toList();
        testResults.forEach(r -> diseaseIds.add(r.diseaseId()));
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
        Map<TermId, Set<SimpleTerm>> maxoToHpoTermMap = DifferentialDiagnosis.makeMaxoToHpoTermMap(hpo, hpoToMaxoTermMap);

        LOGGER.info(maxoToHpoTermMap.toString());

        return maxoToHpoTermMap;
    }

    public Map<TermId, Double> makeMaxoScoreMap(Map<TermId, Set<SimpleTerm>> maxoToHpoTermMap, AnalysisResults results, Double weight) {
        return DifferentialDiagnosis.makeMaxoScoreMap(maxoToHpoTermMap, diseases, results, weight);
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
