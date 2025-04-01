package org.monarchinitiative.maxodiff.cli.cmd;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbabilities;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.config.MaxodiffPropsConfiguration;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementOptions;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementResults;
import org.monarchinitiative.maxodiff.lirical.PhenopacketFileParser;
import org.monarchinitiative.maxodiff.lirical.*;
import org.monarchinitiative.maxodiff.core.model.*;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;


/**
 * Perform Differential Diagnosis calculations
 */

@CommandLine.Command(name = "diagnosis", aliases = {"d"},
        mixinStandardHelpOptions = true,
        description = "maxodiff analysis")
public class DifferentialDiagnosisCommand extends BaseCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DifferentialDiagnosisCommand.class);

    @CommandLine.Option(names = {"-m", "--maxoData"},
            required = true,
            description = "Path to MaXo data directory.")
    protected Path maxoDataPath;

    @CommandLine.Option(names = {"-p", "--phenopacket"},
//            required = true,
//            arity = "1..*",
            description = "Path(s) to phenopacket JSON file(s).")
    protected Path phenopacketPath;

    @CommandLine.Option(names = {"-O", "--outputDirectory"},
//            required = true,
            description = "Where to write the results files.")
    protected Path outputDir;

    @CommandLine.Option(names = {"--format"},
            paramLabel = "{tsv,html,json}",
            description = "LIRICAL results output format (default: ${DEFAULT-VALUE}).")
    protected String outputFormatArg = "tsv";

    @CommandLine.Option(names = {"--compress"},
            description = "Whether to output LIRICAL results file as a compressed file (default: ${DEFAULT-VALUE}).")
    protected boolean compress = false;

    @CommandLine.Option(names = {"-w", "--weight"},
//            split=",",
//            arity = "1..*",
            description = "Comma-separated list of weight value to use in final score calculation (default: ${DEFAULT-VALUE}).")
    public Double weightsArg;

    @CommandLine.Option(names = {"-l", "--diseaseList"},
//            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of diseases to include in differential diagnosis.")
    protected List<String> diseaseIdsArg;

    @CommandLine.Option(names = {"-n", "--nDiseases"},
//            required = true,
//            split=",",
//            arity = "1..*",
            description = "Comma-separated list of n diseases for filtering diseases to include in differential diagnosis.")
    protected Integer nDiseasesArg;

    @CommandLine.Option(names = {"--diseaseProbModel"},
            paramLabel = "{ranked,softmax,expDecay}",
            description = "Disease Probability Model to use for Rank MAxO algorithm (default: ${DEFAULT-VALUE}).")
    protected String diseaseProbModel = "ranked";

    @CommandLine.Option(names = {"-nr", "--nRepetitions"},
//            required = true,
//            split=",",
//            arity = "1..*",
            description = "Number of repetitions for running differential diagnosis.")
    protected Integer nRepetitionsArg;

    @Override
    public Integer execute() throws Exception {

        String phenopacketName = phenopacketPath.toFile().getName();
        String outputFilename = String.join("_", phenopacketName, "maxodiff", "results.csv");
        Path maxodiffResultsFilePath = Path.of(String.join(File.separator, outputDir.toString(), outputFilename));

        double weight = weightsArg;
        int nDiseases = nDiseasesArg;
        int nRepetitions = nRepetitionsArg;

        try (BufferedWriter writer = openOutputFileWriter(maxodiffResultsFilePath); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            runSingleMaxodiffAnalysis(phenopacketPath, phenopacketName, nDiseases, nRepetitions, weight, true, printer);
        }

        return 0;
    }

    protected void runSingleMaxodiffAnalysis(Path phenopacketPath, String phenopacketName, int nDiseases, int nRepetitions, double weight,
                                             boolean writeOutputFile, CSVPrinter printer) throws Exception {


        if (writeOutputFile) {
            printer.printRecord("phenopacket", "disease_id", "maxo_id", "maxo_label",
                    "n_diseases", "disease_ids", "n_repetitions", "weight", "score"); // header
        }

        Map<String, List<Object>> resultsMap = new HashMap<>();

        resultsMap.put("phenopacketName", new ArrayList<>());
        resultsMap.put("diseaseId", new ArrayList<>());
        resultsMap.put("maxScoreMaxoTermId", new ArrayList<>());
        resultsMap.put("maxScoreTermLabel", new ArrayList<>());
        resultsMap.put("topNDiseases", new ArrayList<>());
        resultsMap.put("diseaseIds", new ArrayList<>());
        resultsMap.put("nRepetitions", new ArrayList<>());
        resultsMap.put("weight", new ArrayList<>());
        resultsMap.put("maxScoreValue", new ArrayList<>());


        System.out.println(weight);
        System.out.println(nDiseases);
        System.out.println(nRepetitions);

        Lirical lirical = prepareLirical();
        PhenotypeService phenotypeService = lirical.phenotypeService();
        Set<TermId> liricalDiseaseIds = lirical.phenotypeService().diseases().diseaseIds();

        try (MaxodiffLiricalAnalysisRunner maxodiffLiricalAnalysisRunner =
                     MaxodiffLiricalAnalysisRunnerImpl.of(phenotypeService, 4)) {
            LiricalDifferentialDiagnosisEngineConfigurer liricalDifferentialDiagnosisEngineConfigurer = LiricalDifferentialDiagnosisEngineConfigurer.of(maxodiffLiricalAnalysisRunner);
            var analysisOptions = AnalysisOptions.builder()
                    .useStrictPenalties(runConfiguration.strict)
                    .useGlobal(runConfiguration.globalAnalysisMode)
                    .pretestProbability(PretestDiseaseProbabilities.uniform(liricalDiseaseIds))
                    .build();
            LiricalDifferentialDiagnosisEngine engine = liricalDifferentialDiagnosisEngineConfigurer.configure(analysisOptions);

            PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(phenopacketPath);
            Sample sample = Sample.of(phenopacketData.sampleId(),
                    phenopacketData.presentHpoTermIds().toList(),
                    phenopacketData.excludedHpoTermIds().toList());

            // Get initial differential diagnoses from running LIRICAL
            List<DifferentialDiagnosis> differentialDiagnoses = engine.run(sample);

            // Make maxodiffRefiner
            MaxodiffDataResolver maxodiffDataResolver = MaxodiffDataResolver.of(maxoDataPath);
            MaxodiffPropsConfiguration maxodiffPropsConfiguration = MaxodiffPropsConfiguration.createConfig(maxodiffDataResolver);

            DiffDiagRefiner maxoDiffRefiner = maxodiffPropsConfiguration.diffDiagRefiner("score");
            BiometadataService biometadataService = maxodiffPropsConfiguration.biometadataService();

            //TODO? get list of diseases from LIRICAL results, and add diseases from CLI arg to total list for analysis

//            System.out.println(weights);
//            System.out.println(nDiseasesList);

//            for (int nDiseases : nDiseasesList) {
            System.out.println("n Diseases = " + nDiseases);
            // Make MaXo:HPO Term Map
//                Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoTermMap = maxoTermMap.makeMaxoToHpoTermMap(results, null,
//                        phenopacketPath, nDiseases);
//
//                LOGGER.info(String.valueOf(maxoToHpoTermMap));

//                for (double weight : weights) {
            System.out.println("Weight = " + weight);
            // Get List of Refinement results: maxo term scores and frequencies
            RefinementOptions options = RefinementOptions.of(nDiseases, nRepetitions, weight);
            List<DifferentialDiagnosis> orderedDiagnoses = maxoDiffRefiner.getOrderedDiagnoses(differentialDiagnoses, options);
            List<HpoDisease> diseases = maxoDiffRefiner.getDiseases(orderedDiagnoses);
            Map<TermId, List<HpoFrequency>> hpoTermCounts = maxoDiffRefiner.getHpoTermCounts(diseases);

            HpoDiseases hpoDiseases = phenotypeService.diseases();
            List<DifferentialDiagnosis> initialDiagnoses = differentialDiagnoses.subList(0, nDiseases);
            Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap = maxodiffPropsConfiguration.maxoAnnotsMap();
            Map<TermId, Set<TermId>> maxoToHpoTermIdMap = maxoDiffRefiner.getMaxoToHpoTermIdMap(List.of(), hpoTermCounts);

            DiseaseModelProbability diseaseModelProbability = null;
            switch (diseaseProbModel) {
                case "ranked" -> diseaseModelProbability = DiseaseModelProbability.ranked(initialDiagnoses);
                case "softmax" -> diseaseModelProbability = DiseaseModelProbability.softmax(initialDiagnoses);
                case "expDecay" ->
                        diseaseModelProbability = DiseaseModelProbability.exponentialDecay(initialDiagnoses);
            }

            MaxoHpoTermProbabilities maxoHpoTermProbabilities =
                    new MaxoHpoTermProbabilities(hpoDiseases,
                            hpoToMaxoTermMap,
                            initialDiagnoses,
                            diseaseModelProbability);

            Set<TermId> initialDiagnosesIds = initialDiagnoses.stream()
                    .map(DifferentialDiagnosis::diseaseId)
                    .collect(Collectors.toSet());

            var diseaseSubsetOptions = AnalysisOptions.builder()
                    .useStrictPenalties(runConfiguration.strict)
                    .useGlobal(runConfiguration.globalAnalysisMode)
                    .pretestProbability(PretestDiseaseProbabilities.uniform(initialDiagnosesIds))
                    .addTargetDiseases(initialDiagnosesIds)
                    .build();
            LiricalDifferentialDiagnosisEngine diseaseSubsetEngine = liricalDifferentialDiagnosisEngineConfigurer.configure(diseaseSubsetOptions);

            RankMaxo rankMaxo = new RankMaxo(hpoToMaxoTermMap, maxoToHpoTermIdMap, maxoHpoTermProbabilities, diseaseSubsetEngine);

            RefinementResults refinementResults = maxoDiffRefiner.run(sample,
                    orderedDiagnoses,
                    options,
                    rankMaxo,
                    hpoTermCounts,
                    maxoToHpoTermIdMap);
            List<MaxodiffResult> resultsList = new ArrayList<>(refinementResults.maxodiffResults().stream().toList());
            resultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.maxoTermScore().scoreDiff()).reversed());

            TermId diseaseId = phenopacketData.diseaseIds().getFirst();
            // Take the MaXo term that has the highest score
            MaxodiffResult topResult = resultsList.getFirst();
            String maxScoreMaxoTermId = topResult.maxoTermScore().maxoId();
            String maxScoreTermLabel = biometadataService.maxoLabel(maxScoreMaxoTermId).orElse("unknown");
            double maxScoreValue = topResult.maxoTermScore().scoreDiff();

            System.out.println("Max Score: " + maxScoreMaxoTermId + " (" + maxScoreTermLabel + ")" + " = " + maxScoreValue);

            Set<TermId> diseaseIds = topResult.maxoTermScore().omimTermIds();
            int topNDiseases = diseaseIds.size();

            if (writeOutputFile) {
                writeResults(phenopacketName, diseaseId, TermId.of(maxScoreMaxoTermId), maxScoreTermLabel,
                        topNDiseases, diseaseIds.toString(), nRepetitions, weight, maxScoreValue, printer);
            }

            List<Object> phenopacketNames = resultsMap.get("phenopacketName");
            phenopacketNames.add(phenopacketName);
            List<Object> diseaseIdList = resultsMap.get("diseaseId");
            diseaseIdList.add(diseaseId);
            List<Object> maxScoreMaxoTermIds = resultsMap.get("maxScoreMaxoTermId");
            maxScoreMaxoTermIds.add(maxScoreMaxoTermId);
            List<Object> maxScoreTermLabels = resultsMap.get("maxScoreTermLabel");
            maxScoreTermLabels.add(maxScoreTermLabel);
            List<Object> topNDiseasesList = resultsMap.get("topNDiseases");
            topNDiseasesList.add(topNDiseases);
            List<Object> diseaseIdsList = resultsMap.get("diseaseIds");
            diseaseIdsList.add(diseaseIds);
            List<Object> nRepList = resultsMap.get("nRepetitions");
            nRepList.add(nRepetitions);
            List<Object> weightList = resultsMap.get("weight");
            weightList.add(weight);
            List<Object> maxScoreValues = resultsMap.get("maxScoreValue");
            maxScoreValues.add(maxScoreValue);
            resultsMap.replace("phenopacketName", phenopacketNames);
            resultsMap.replace("diseaseId", diseaseIdList);
            resultsMap.replace("maxScoreMaxoTermId", maxScoreMaxoTermIds);
            resultsMap.replace("maxScoreTermLabel", maxScoreTermLabels);
            resultsMap.replace("topNDiseases", topNDiseasesList);
            resultsMap.replace("diseaseIds", diseaseIdsList);
            resultsMap.replace("nRepetitions", nRepList);
            resultsMap.replace("weight", weightList);
            resultsMap.replace("maxScoreValue", maxScoreValues);
//                }
//            }
            BatchDiagnosisCommand.setResultsMap(resultsMap);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            resultsMap = new HashMap<>();
            BatchDiagnosisCommand.setResultsMap(resultsMap);
        }

    }


    protected static BufferedWriter openOutputFileWriter(Path outputPath) throws IOException {
        return outputPath.toFile().getName().endsWith(".gz")
                ? new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(outputPath))))
                : Files.newBufferedWriter(outputPath);
    }



    /**
     * Write results of a single maxodiff analysis into the provided {@code printer}.
     */
    protected static void writeResults(String phenopacketName,
                                     TermId diseaseId,
                                     TermId maxoId,
                                     String maxoLabel,
                                     int topNdiseases,
                                     String diseaseIds,
                                     int nRepetitions,
                                     double weight,
                                     double score,
                                     CSVPrinter printer) {

        try {
            printer.print(phenopacketName);
            printer.print(diseaseId);
            printer.print(maxoId);
            printer.print(maxoLabel);
            printer.print(topNdiseases);
            printer.print(diseaseIds);
            printer.print(nRepetitions);
            printer.print(weight);
            printer.print(score);
            printer.println();
        } catch (IOException e) {
            LOGGER.error("Error writing results for {}: {}", diseaseId, e.getMessage(), e);
        }
    }




}
