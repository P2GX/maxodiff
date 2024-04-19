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

    public record MaxoTerm(String maxoTerm, String maxoLabel, Integer nOmimTerms, String omimTerms, String hpoTerms, Double score) {}

    public record Frequencies(String hpoId, List<String> frequencies) {}

    MaxodiffDataResolver dataResolver;
    Ontology hpo;
    MaxoDxAnnots maxoDxAnnots;
    Map<SimpleTerm, Set<SimpleTerm>> fullHpoToMaxoTermMap;

    AnalysisResults results;
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

    public List<MaxoTerm> getMaxoTermRecords(LiricalAnalysis liricalAnalysis, DifferentialDiagnosis diffDiag, Path phenopacketPath,
                                             double posttestFilter, double weight) throws Exception {
        List<MaxoTerm> maxoTermRecords = new ArrayList<>();
        AnalysisResults results = liricalAnalysis.runLiricalAnalysis(phenopacketPath);
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = makeMaxoToHpoTermIdMap(results, diffDiag, phenopacketPath, posttestFilter);
        Map<TermId, Double> maxoScoreMap = makeMaxoScoreMap(diffDiag, maxoToHpoTermIdMap, weight);
        LOGGER.info(maxoScoreMap.toString());
        maxoToHpoTermIdMap.forEach((key, value) -> {
            String maxoId = key.toString();
            String maxoTermLabel = diffDiag.getMaxoTermLabel(hpoToMaxoTermMap, key);
            int nOmimTerms = diseases.size();
            Set<TermId> omimIds = new HashSet<>();
            diseases.forEach(disease -> omimIds.add(disease.id()));
            String omimTerms = omimIds.toString();
            String hpoTerms = value.toString();
            double score = maxoScoreMap.get(key);
            maxoTermRecords.add(new MaxoTerm(maxoId, maxoTermLabel, nOmimTerms, omimTerms, hpoTerms, score));
            Comparator<MaxoTerm> comp = Comparator.comparing(MaxoTerm::score, Comparator.reverseOrder());
            maxoTermRecords.sort(comp);
        });
        return maxoTermRecords;
    }

    public List<Frequencies> getFrequencyRecords(MaxoTerm maxoTermRecord) {
        List<Frequencies> frequencyRecords = new ArrayList<>();
        List<String> omimIds = new ArrayList<>(Arrays.asList(maxoTermRecord.omimTerms
                .replaceAll("\\[", "")
                .replaceAll("\\]","")
                .replaceAll(" ", "")
                .split(",")));
        List<String> hpoIds = new ArrayList<>(Arrays.asList(maxoTermRecord.hpoTerms
                .replaceAll("\\[", "")
                .replaceAll("\\]","")
                .replaceAll(" ", "")
                .split(",")));
        for (String hpoId : hpoIds) {
            Map<String, String> maxoFrequencies = new LinkedHashMap<>();
            omimIds.forEach(e -> maxoFrequencies.put(e, "-"));
            List<HpoFrequency> frequencies = hpoTermCounts.get(TermId.of(hpoId));
            for (HpoFrequency hpoFrequency : frequencies) {
                for (String omimId : omimIds) {
                    if (hpoFrequency.omimId().equals(omimId)) {
                        maxoFrequencies.replace(omimId, hpoFrequency.frequency().toString());
                    }
                }
            }
            frequencyRecords.add(new Frequencies(hpoId, maxoFrequencies.values().stream().toList()));
        }
        return frequencyRecords;
    }

    public Map<TermId, Set<TermId>> makeMaxoToHpoTermIdMap(AnalysisResults analysisResults, DifferentialDiagnosis diffDiag,
                                                           Path phenopacketPath, double posttestFilter) throws Exception {

        PhenopacketData phenopacketData = LiricalAnalysis.readPhenopacketData(phenopacketPath);
        diseaseId = phenopacketData.diseaseIds().get(0);
        results = analysisResults;
        LOGGER.info("Min Posttest Probabiltiy Threshold = " + posttestFilter);
        // Collect HPO terms and frequencies for the target m diseases
        List<TermId> diseaseIds = new ArrayList<>();
        List<TestResult> testResults = results.resultsWithDescendingPostTestProbability()
                .filter(r -> r.posttestProbability() >= posttestFilter)
                .toList();
        testResults.forEach(r -> diseaseIds.add(r.diseaseId()));
        LOGGER.info(phenopacketPath + " diseaseIds: " + String.valueOf(diseaseIds));
        int topNDiseases = diseaseIds.size();

        diseases = diffDiag.makeDiseaseList(dataResolver, diseaseIds);
        DiseaseTermCount diseaseTermCount = DiseaseTermCount.of(diseases);
        hpoTermCounts = diseaseTermCount.hpoTermCounts();

        // Remove HPO terms present in the phenopacket
        phenopacketData.presentHpoTermIds().forEach(hpoTermCounts::remove);
        phenopacketData.excludedHpoTermIds().forEach(hpoTermCounts::remove);

        // Get all the MaXo terms that can be used to diagnose the HPO terms
        hpoToMaxoTermMap = diffDiag.makeHpoToMaxoTermMap(fullHpoToMaxoTermMap, hpoTermCounts.keySet());
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = diffDiag.makeMaxoToHpoTermIdMap(hpo, hpoToMaxoTermMap);

        LOGGER.info(maxoToHpoTermIdMap.toString());

        return maxoToHpoTermIdMap;
    }

    public Map<TermId, Double> makeMaxoScoreMap(DifferentialDiagnosis diffDiag, Map<TermId, Set<TermId>> maxoToHpoTermIdMap, Double weight) {
        return diffDiag.makeMaxoScoreMap(maxoToHpoTermIdMap, diseases, results, weight);
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
