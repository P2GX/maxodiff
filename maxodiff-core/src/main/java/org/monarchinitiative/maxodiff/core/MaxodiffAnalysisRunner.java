package org.monarchinitiative.maxodiff.core;

import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementResults;
import org.monarchinitiative.maxodiff.core.model.PhenopacketData;
import org.monarchinitiative.maxodiff.core.model.RankMaxo;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementOptions;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;

public class MaxodiffAnalysisRunner {
    private final static Logger LOGGER = LoggerFactory.getLogger(MaxoDiffAnalysisResultRow.class);
    private final int nDiseases;
    private final int nRepetitions;
    private final DifferentialDiagnosisEngine engine;
    private final DiffDiagRefiner maxoDiffRefiner;
    private final BiometadataService biometadataService;

    public MaxodiffAnalysisRunner(
            int nDiseases,
            int nRepetitions,
            DifferentialDiagnosisEngine engine,
            DiffDiagRefiner maxoDiffRefiner,
            BiometadataService biometadataService
    ) {
        this.nDiseases = nDiseases;
        this.nRepetitions = nRepetitions;
        this.engine = engine;
        this.maxoDiffRefiner = maxoDiffRefiner;
        this.biometadataService = biometadataService;
    }

    public MaxodiffAnalysisResult analyzeSample(PhenopacketData ppktData) throws Exception {
        Sample sample = ppktData.getSample();
        List<DifferentialDiagnosis> differentialDiagnoses = engine.run(sample);
        // Get List of Refinement results: maxo term scores and frequencies
        RefinementOptions options = RefinementOptions.of(this.nDiseases, this.nRepetitions);
        List<DifferentialDiagnosis> orderedDiagnoses = maxoDiffRefiner.getOrderedDiagnoses(differentialDiagnoses, options);
        List<HpoDisease> diseases = maxoDiffRefiner.getDiseases(orderedDiagnoses);
        Map<TermId, List<HpoFrequency>> hpoTermCounts = maxoDiffRefiner.getHpoTermCounts(diseases);
        RefinementResults refinementResults = getRefinementResults(
                differentialDiagnoses, orderedDiagnoses, hpoTermCounts, sample);
        // refinement results already sorted in object
        var results = new ArrayList<>(refinementResults.maxodiffResults().stream().toList());
        return new MaxodiffAnalysisResult(results, hpoTermCounts);
    }

    public MaxoDiffAnalysisResultRow batchAnalysis(PhenopacketData ppktData) throws Exception {
        MaxodiffAnalysisResult result = analyzeSample(ppktData);
        List<MaxodiffResult> resultsList = result.results();
        String phenopacket_id = ppktData.sampleId();
        String disease_id = ppktData.diseaseIds().getFirst().getValue();
        MaxodiffResult topResult = resultsList.getFirst();
        String maxo_id = topResult.rankMaxoScore().maxoId().toString();
        String maxo_label = biometadataService.maxoLabel(maxo_id).orElse("unknown");
        double maxScoreValue = topResult.rankMaxoScore().maxoScore();
        Set<TermId> diseaseIds = topResult.rankMaxoScore().maxoOmimTermIds();

        return new MaxoDiffAnalysisResultRow(
                phenopacket_id,
                disease_id,
                maxo_id,
                maxo_label,
                this.nDiseases,
                diseaseIds,
                this.nRepetitions,
                maxScoreValue
        );
    }


    private RefinementResults getRefinementResults(
            List<DifferentialDiagnosis> differentialDiagnoses,
            List<DifferentialDiagnosis> orderedDiagnoses,
            Map<TermId, List<HpoFrequency>> hpoTermCounts,
            Sample sample) throws Exception {
        RefinementOptions options = RefinementOptions.of(this.nDiseases, nRepetitions);
        List<DifferentialDiagnosis> allOrderedDiagnoses = differentialDiagnoses.stream()
                .sorted(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed())
                .toList();

        List<DifferentialDiagnosis> initialDiagnoses = orderedDiagnoses.subList(0, options.nDiseases());
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = maxoDiffRefiner.getMaxoToHpoTermIdMap(hpoTermCounts);

        String diseaseProbModel = "ranked"; // TODO -- Can we make this an enum?
        RankMaxo rankMaxo = maxoDiffRefiner.getRankMaxo(allOrderedDiagnoses, initialDiagnoses, engine, maxoToHpoTermIdMap, diseaseProbModel);

        return maxoDiffRefiner.run(sample,
                orderedDiagnoses,
                options,
                rankMaxo,
                hpoTermCounts,
                maxoToHpoTermIdMap);
    }


}


/*

    protected void runSingleMaxodiffAnalysis(Path phenopacketPath,
                                             String phenopacketName,
                                             int nDiseases,
                                             int nRepetitions,
                                             ScoringMode scoringMode,
                                             boolean writeCsvFile,
                                             CSVPrinter printer) throws Exception {

        // Load ontology and hpo diseases
        MaxoDiffLoader mdloader =  MaxoDiffLoader.fileLoader(maxoDataPath);
        HpoDiseases hpoDiseases = mdloader.hpoDiseases();
        IcMicaData icMicaData = mdloader.icMicaData();

        // Initialize results map and output file
        Map<String, List<Object>> resultsMap = new HashMap<>();

        resultsMap.put("phenopacketName", new ArrayList<>());
        resultsMap.put("diseaseId", new ArrayList<>());
        resultsMap.put("maxScoreMaxoTermId", new ArrayList<>());
        resultsMap.put("maxScoreTermLabel", new ArrayList<>());
        resultsMap.put("topNDiseases", new ArrayList<>());
        resultsMap.put("diseaseIds", new ArrayList<>());
        resultsMap.put("nRepetitions", new ArrayList<>());
        resultsMap.put("maxScoreValue", new ArrayList<>());

        if (writeCsvFile) {
            printer.printRecord("phenopacket", "disease_id", "maxo_id", "maxo_label",
                    "n_diseases", "disease_ids", "n_repetitions", "score"); // header
        }


        String outputFilename = null;
        try {
            // Make maxodiffRefiner
            MaxodiffDataResolver maxodiffDataResolver = MaxodiffDataResolver.of(maxoDataPath);
            MaxodiffPropsConfiguration maxodiffPropsConfiguration = MaxodiffPropsConfiguration.createConfig(maxodiffDataResolver);

            DiffDiagRefiner maxoDiffRefiner = maxodiffPropsConfiguration.diffDiagRefiner("score");
            BiometadataService biometadataService = maxodiffPropsConfiguration.biometadataService();

            // Configure Phenomizer engine
            Map<TermPair, Double> icMicaDict = icMicaData.icMicaDict();
            DifferentialDiagnosisEngine engine = new PhenomizerDifferentialDiagnosisEngine(hpoDiseases, icMicaDict, scoringMode);

            // Read phenopacket data and make sample
            PhenopacketData phenopacketData = PhenopacketData.readPhenopacketData(phenopacketPath);
            Sample sample = phenopacketData.getSample();

            // Get initial differential diagnoses
            List<DifferentialDiagnosis> differentialDiagnoses = engine.run(sample);

            // Get List of Refinement results: maxo term scores and frequencies
            RefinementOptions options = RefinementOptions.of(nDiseases, nRepetitions);
            List<DifferentialDiagnosis> orderedDiagnoses = maxoDiffRefiner.getOrderedDiagnoses(differentialDiagnoses, options);
            List<HpoDisease> diseases = maxoDiffRefiner.getDiseases(orderedDiagnoses);
            Map<TermId, List<HpoFrequency>> hpoTermCounts = maxoDiffRefiner.getHpoTermCounts(diseases);

            RefinementResults refinementResults = getRefinementResults(options, maxoDiffRefiner,
                    differentialDiagnoses, orderedDiagnoses, hpoTermCounts, engine, sample);

            // Sort refinement results and write to file
            List<MaxodiffResult> resultsList = new ArrayList<>(refinementResults.maxodiffResults().stream().toList());
            resultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.rankMaxoScore().maxoScore()).reversed());

            TermId diseaseId = phenopacketData.diseaseIds().getFirst();
            // Take the MaXo term that has the highest score
            MaxodiffResult topResult = resultsList.getFirst();
            String maxScoreMaxoTermId = topResult.rankMaxoScore().maxoId().toString();
            String maxScoreTermLabel = biometadataService.maxoLabel(maxScoreMaxoTermId).orElse("unknown");
            double maxScoreValue = topResult.rankMaxoScore().maxoScore();

            System.out.println();
            System.out.println("Max Score: " + maxScoreMaxoTermId + " (" + maxScoreTermLabel + ")" + " = " + maxScoreValue);

            Set<TermId> diseaseIds = topResult.rankMaxoScore().maxoOmimTermIds();
            int topNDiseases = diseaseIds.size();

            String nDiseasesAbbr = String.join("", "n", String.valueOf(options.nDiseases()));
            String nRepsAbbr = String.join("", "nr", String.valueOf(options.nRepetitions()));
            outputFilename = String.join("_", phenopacketName,
                    nDiseasesAbbr, nRepsAbbr, "maxodiff", "results.html");

            writeRefinementResults(resultsList, diseaseId, maxScoreMaxoTermId, maxScoreTermLabel, topNDiseases,
                                diseaseIds, maxScoreValue, biometadataService, writeCsvFile, phenopacketName,
                                options, printer, outputDir, outputFilename, sample, hpoDiseases, hpoTermCounts,
                                icMicaDict);

            resultsMap.get("phenopacketName").add(phenopacketName);
            resultsMap.get("diseaseId").add(diseaseId);
            resultsMap.get("maxScoreMaxoTermId").add(maxScoreMaxoTermId);
            resultsMap.get("maxScoreTermLabel").add(maxScoreTermLabel);
            resultsMap.get("topNDiseases").add(topNDiseases);
            resultsMap.get("diseaseIds").add(diseaseIds);
            resultsMap.get("nRepetitions").add(nRepetitions);
            resultsMap.get("maxScoreValue").add(maxScoreValue);

            BatchDiagnosisCommand.setResultsMap(resultsMap);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return;
        }
        System.out.println("Wrote output to " + outputFilename);
    }
 */
