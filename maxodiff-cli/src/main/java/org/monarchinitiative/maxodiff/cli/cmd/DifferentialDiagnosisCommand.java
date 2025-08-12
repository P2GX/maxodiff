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
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.html.results.HtmlResults;
import org.monarchinitiative.maxodiff.lirical.PhenopacketFileParser;
import org.monarchinitiative.maxodiff.lirical.*;
import org.monarchinitiative.maxodiff.core.model.*;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.phenomizer.IcMicaData;
import org.monarchinitiative.maxodiff.phenomizer.IcMicaDictLoader;
import org.monarchinitiative.maxodiff.phenomizer.PhenomizerDifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.phenomizer.ScoringMode;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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
            description = "Path to MaXo data directory.")
    protected Path maxoDataPath = Path.of("data");

    @CommandLine.Option(names = {"-p", "--phenopacket"},
            description = "Path(s) to phenopacket JSON file(s).")
    protected Path phenopacketPath;

    @CommandLine.Option(names = {"-O", "--outputDirectory"},
//            required = true,
            description = "Where to write the results files.")
    protected Path outputDir = Path.of(".");

    @CommandLine.Option(names = {"--format"},
            paramLabel = "{tsv,html,json}",
            description = "LIRICAL results output format (default: ${DEFAULT-VALUE}).")
    protected String outputFormatArg = "tsv";

    @CommandLine.Option(names = {"--compress"},
            description = "Whether to output LIRICAL results file as a compressed file (default: ${DEFAULT-VALUE}).")
    protected boolean compress = false;

    @CommandLine.Option(names = {"-l", "--diseaseList"},
            split=",",
            arity = "1..*",
            description = "Comma-separated list of diseases to include in differential diagnosis.")
    protected List<String> diseaseIdsArg;

    @CommandLine.Option(names = {"-n", "--nDiseases"},
            description = "Comma-separated list of n diseases for filtering diseases to include in differential diagnosis.")
    protected Integer nDiseasesArg = 20;

    @CommandLine.Option(names = {"--diseaseProbModel"},
            paramLabel = "{ranked,softmax,expDecay}",
            description = "Disease Probability Model to use for Rank MAxO algorithm (default: ${DEFAULT-VALUE}).")
    protected String diseaseProbModel = "ranked";

    @CommandLine.Option(names = {"-nr", "--nRepetitions"},
            description = "Number of repetitions for running differential diagnosis.")
    protected Integer nRepetitionsArg = 10;

    @CommandLine.Option(names = {"-e", "--engine"},
            paramLabel = "{lirical, phenomizer}",
            description = "Differential diagnosis engine (default: ${DEFAULT-VALUE}).")
    protected String engineArg = "lirical";

    @CommandLine.Option(names = {"-s", "--scoringMode"},
            paramLabel = "{one-sided, two-sided}",
            description = "Phenomizer scoring mode (default: ${DEFAULT-VALUE}).")
    protected String scoringModeArg = "one-sided";

    @Override
    public Integer execute() throws Exception {

        String phenopacketName = phenopacketPath.toFile().getName();
        String outputFilename = String.join("_", phenopacketName, "maxodiff", "results.csv");
        Path maxodiffResultsFilePath = Path.of(String.join(File.separator, outputDir.toString(), outputFilename));

        int nDiseases = nDiseasesArg;
        int nRepetitions = nRepetitionsArg;
        String ddEngine = engineArg;
        ScoringMode scoringMode = scoringModeArg.equals("one-sided") ? ScoringMode.ONE_SIDED : ScoringMode.TWO_SIDED;

        try (BufferedWriter writer = openOutputFileWriter(maxodiffResultsFilePath); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            runSingleMaxodiffAnalysis(phenopacketPath, phenopacketName, nDiseases, nRepetitions, ddEngine, scoringMode, true, printer);
        }

        return 0;
    }

    protected void runSingleMaxodiffAnalysis(Path phenopacketPath, String phenopacketName, int nDiseases, int nRepetitions,
                                             String ddEngine, ScoringMode scoringMode, boolean writeOutputFile, CSVPrinter printer) throws Exception {


        Path hpoPath = MaxodiffDataResolver.of(maxoDataPath).hpoJson();
        Ontology ontology = OntologyLoader.loadOntology(hpoPath.toFile());
        MinimalOntology minimalOntology = MinimalOntologyLoader.loadOntology(hpoPath.toFile());
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(minimalOntology, HpoDiseaseLoaderOptions.defaultOptions());

        Path hpoaPath = MaxodiffDataResolver.of(maxoDataPath).phenotypeAnnotations();
        HpoDiseases hpoDiseases = loader.load(hpoaPath);

        IcMicaData icMicaData = null;
        if (ddEngine.equals("phenomizer")) {
            LOGGER.info("Loading icMicaDict...");
            try {
                icMicaData = IcMicaDictLoader.loadIcMicaDict(MaxodiffDataResolver.of(maxoDataPath).icMicaDict());
            } catch (NoSuchFileException ex) {
                throw new Exception(String.join(". ", ex.getMessage(), "Run Download command to download the necessary term-pair-similarity file."));
            }
        }

        if (writeOutputFile) {
            printer.printRecord("phenopacket", "disease_id", "maxo_id", "maxo_label",
                    "n_diseases", "disease_ids", "n_repetitions", "score"); // header
        }

        Map<String, List<Object>> resultsMap = new HashMap<>();

        resultsMap.put("phenopacketName", new ArrayList<>());
        resultsMap.put("diseaseId", new ArrayList<>());
        resultsMap.put("maxScoreMaxoTermId", new ArrayList<>());
        resultsMap.put("maxScoreTermLabel", new ArrayList<>());
        resultsMap.put("topNDiseases", new ArrayList<>());
        resultsMap.put("diseaseIds", new ArrayList<>());
        resultsMap.put("nRepetitions", new ArrayList<>());
        resultsMap.put("maxScoreValue", new ArrayList<>());


        System.out.println(nDiseases);
        System.out.println(nRepetitions);

        try {
            // Make maxodiffRefiner
            MaxodiffDataResolver maxodiffDataResolver = MaxodiffDataResolver.of(maxoDataPath);
            MaxodiffPropsConfiguration maxodiffPropsConfiguration = MaxodiffPropsConfiguration.createConfig(maxodiffDataResolver);

            DiffDiagRefiner maxoDiffRefiner = maxodiffPropsConfiguration.diffDiagRefiner("score");
            BiometadataService biometadataService = maxodiffPropsConfiguration.biometadataService();

            DifferentialDiagnosisEngine engine = null;
            LiricalDifferentialDiagnosisEngineConfigurer liricalDifferentialDiagnosisEngineConfigurer = null;
            if (ddEngine.equals("lirical")) {
                Lirical lirical = prepareLirical();
                PhenotypeService phenotypeService = lirical.phenotypeService();
                Set<TermId> liricalDiseaseIds = lirical.phenotypeService().diseases().diseaseIds();
                MaxodiffLiricalAnalysisRunner maxodiffLiricalAnalysisRunner = MaxodiffLiricalAnalysisRunnerImpl.of(phenotypeService, 4);
                liricalDifferentialDiagnosisEngineConfigurer = LiricalDifferentialDiagnosisEngineConfigurer.of(maxodiffLiricalAnalysisRunner);
                var analysisOptions = AnalysisOptions.builder()
                        .useStrictPenalties(runConfiguration.strict)
                        .useGlobal(runConfiguration.globalAnalysisMode)
                        .pretestProbability(PretestDiseaseProbabilities.uniform(liricalDiseaseIds))
                        .build();
                engine = liricalDifferentialDiagnosisEngineConfigurer.configure(analysisOptions);
            } else if (ddEngine.equals("phenomizer")) {
                Map<TermPair, Double> icMicaDict = icMicaData.icMicaDict();
                engine = new PhenomizerDifferentialDiagnosisEngine(hpoDiseases, icMicaDict, scoringMode);
            }


            PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(phenopacketPath);
            Sample sample = Sample.of(phenopacketData.sampleId(),
                    phenopacketData.presentHpoTermIds().toList(),
                    phenopacketData.excludedHpoTermIds().toList());

            // Get initial differential diagnoses
            assert engine != null;
            List<DifferentialDiagnosis> differentialDiagnoses = engine.run(sample);

            System.out.println("n Diseases = " + nDiseases);

            // Get List of Refinement results: maxo term scores and frequencies
            RefinementOptions options = RefinementOptions.of(nDiseases, nRepetitions);
            List<DifferentialDiagnosis> orderedDiagnoses = maxoDiffRefiner.getOrderedDiagnoses(differentialDiagnoses, options);
            List<HpoDisease> diseases = maxoDiffRefiner.getDiseases(orderedDiagnoses);
            Map<TermId, List<HpoFrequency>> hpoTermCounts = maxoDiffRefiner.getHpoTermCounts(diseases);

            List<DifferentialDiagnosis> initialDiagnoses = orderedDiagnoses.subList(0, nDiseases);
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

            DifferentialDiagnosisEngine diseaseSubsetEngine = null;
            if (ddEngine.equals("lirical")) {
                var diseaseSubsetOptions = AnalysisOptions.builder()
                        .useStrictPenalties(runConfiguration.strict)
                        .useGlobal(runConfiguration.globalAnalysisMode)
                        .pretestProbability(PretestDiseaseProbabilities.uniform(initialDiagnosesIds))
                        .addTargetDiseases(initialDiagnosesIds)
                        .build();
                diseaseSubsetEngine = liricalDifferentialDiagnosisEngineConfigurer.configure(diseaseSubsetOptions);
            } else if (ddEngine.equals("phenomizer")) {
                diseaseSubsetEngine = engine;
            }


            RankMaxo rankMaxo = new RankMaxo(hpoToMaxoTermMap, maxoToHpoTermIdMap, maxoHpoTermProbabilities, diseaseSubsetEngine,
                    minimalOntology, ontology);

            RefinementResults refinementResults = maxoDiffRefiner.run(sample,
                    orderedDiagnoses,
                    options,
                    rankMaxo,
                    hpoTermCounts,
                    maxoToHpoTermIdMap);
            List<MaxodiffResult> resultsList = new ArrayList<>(refinementResults.maxodiffResults().stream().toList());
            resultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.rankMaxoScore().maxoScore()).reversed());

            TermId diseaseId = phenopacketData.diseaseIds().getFirst();
            // Take the MaXo term that has the highest score
            MaxodiffResult topResult = resultsList.getFirst();
            String maxScoreMaxoTermId = topResult.rankMaxoScore().maxoId().toString();
            String maxScoreTermLabel = biometadataService.maxoLabel(maxScoreMaxoTermId).orElse("unknown");
            double maxScoreValue = topResult.rankMaxoScore().maxoScore();

            System.out.println("Max Score: " + maxScoreMaxoTermId + " (" + maxScoreTermLabel + ")" + " = " + maxScoreValue);

            Set<TermId> diseaseIds = topResult.rankMaxoScore().maxoOmimTermIds();
            int topNDiseases = diseaseIds.size();

            if (writeOutputFile) {
                writeResults(phenopacketName, diseaseId, TermId.of(maxScoreMaxoTermId), maxScoreTermLabel,
                        topNDiseases, diseaseIds.toString(), nRepetitions, maxScoreValue, printer);

                String nDiseasesAbbr = String.join("", "n", String.valueOf(nDiseases));
                String nRepsAbbr = String.join("", "nr", String.valueOf(nRepetitions));
                String outputFilename = String.join("_", phenopacketName, ddEngine,
                        nDiseasesAbbr, nRepsAbbr, "maxodiff", "results.html");
                Path maxodiffResultsHTMLPath = Path.of(String.join(File.separator, outputDir.toString(), outputFilename));

                String htmlString = HtmlResults.writeHTMLResults(sample, nDiseases, nRepetitions, resultsList,
                        biometadataService, hpoTermCounts);

                Files.writeString(maxodiffResultsHTMLPath, htmlString);
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
            List<Object> maxScoreValues = resultsMap.get("maxScoreValue");
            maxScoreValues.add(maxScoreValue);

            resultsMap.replace("phenopacketName", phenopacketNames);
            resultsMap.replace("diseaseId", diseaseIdList);
            resultsMap.replace("maxScoreMaxoTermId", maxScoreMaxoTermIds);
            resultsMap.replace("maxScoreTermLabel", maxScoreTermLabels);
            resultsMap.replace("topNDiseases", topNDiseasesList);
            resultsMap.replace("diseaseIds", diseaseIdsList);
            resultsMap.replace("nRepetitions", nRepList);
            resultsMap.replace("maxScoreValue", maxScoreValues);

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
            printer.print(score);
            printer.println();
        } catch (IOException e) {
            LOGGER.error("Error writing results for {}: {}", diseaseId, e.getMessage(), e);
        }
    }


}
