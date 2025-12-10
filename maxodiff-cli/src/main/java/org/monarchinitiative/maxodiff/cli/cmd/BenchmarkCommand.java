package org.monarchinitiative.maxodiff.cli.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.config.MaxodiffPropsConfiguration;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.analysis.refinement.*;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.*;
import org.monarchinitiative.maxodiff.html.results.HtmlResults;
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
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;


/**
 * Perform Differential Diagnosis calculations
 */

@CommandLine.Command(name = "benchmark", aliases = {"B"},
        mixinStandardHelpOptions = true,
        description = "benchmark maxodiff analysis")
public class BenchmarkCommand extends DifferentialDiagnosisCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DifferentialDiagnosisCommand.class);

    private static ObjectMapper OBJECT_MAPPER;

    @CommandLine.Option(names = {"-B", "--batchDir"},
            description = "Path to directory containing phenopackets.")
    protected String batchDir;


    @CommandLine.Option(names = {"-N", "--nDiseasesList"},
//            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of posttest probability thresholds for filtering diseases to include in differential diagnosis.")
    protected List<Integer> nDiseasesArg;

    @CommandLine.Option(names = {"-NR", "--nRepetitionsList"},
//            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of numbers of repetitions for running differential diagnosis.")
    protected List<Integer> nRepetitionsArg;

    @CommandLine.Option(names = {"-o", "--outputFilename"},
            description = "Filename of the benchmark results CSV file. The CSV is compressed if the path has the '.gz' suffix")
    protected Path outputName;



    @Override
    public Integer execute() throws Exception {

        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        OBJECT_MAPPER.registerModule(new Jdk8Module());

        // Load ontology and hpo diseases
        Ontology ontology = OntologyLoader.loadOntology(MaxodiffDataResolver.of(maxoDataPath).hpoJson().toFile());
        MinimalOntology minimalOntology = MinimalOntologyLoader.loadOntology(MaxodiffDataResolver.of(maxoDataPath).hpoJson().toFile());
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(minimalOntology, HpoDiseaseLoaderOptions.defaultOmim());

        Path hpoaPath = MaxodiffDataResolver.of(maxoDataPath).phenotypeAnnotations();
        HpoDiseases hpoDiseases = loader.load(hpoaPath);

        // Set up Phenomizer engine
        String ddEngine = "phenomizer";
        LOGGER.info("Loading icMicaDict...");
        IcMicaData icMicaData = IcMicaDictLoader.loadIcMicaDict(MaxodiffDataResolver.of(maxoDataPath).icMicaDict());

        // Get phenopacket paths from batch directory, or single phenopacket path if provided instead
        List<Path> phenopacketPaths = new ArrayList<>();
        if (batchDir != null) {
            File folder = new File(batchDir);
            File[] files = folder.listFiles();
            assert files != null;
            for (File file : files) {
                BasicFileAttributes basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                if (basicFileAttributes.isRegularFile() && !basicFileAttributes.isDirectory() && !file.getName().startsWith(".")) {
                    phenopacketPaths.add(file.toPath());
                }
            }
        } else if (phenopacketPath != null) {
            phenopacketPaths.add(phenopacketPath);
        }
        Collections.sort(phenopacketPaths);

        // Make N Diseases and Repetitions lists from ClI arguments
        List<Integer> nDiseasesList = new ArrayList<>();
        nDiseasesArg.forEach(nDiseasesList::add);
        List<Integer> nRepetitionsList = new ArrayList<>();
        nRepetitionsArg.forEach(nRepetitionsList::add);

        try {
            // Make maxodiffRefiner
            MaxodiffDataResolver maxodiffDataResolver = MaxodiffDataResolver.of(maxoDataPath);
            MaxodiffPropsConfiguration maxodiffPropsConfiguration = MaxodiffPropsConfiguration.createConfig(maxodiffDataResolver);

            BiometadataService biometadataService = maxodiffPropsConfiguration.biometadataService();

            // Configure Phenomizer engine
            ScoringMode scoringMode = scoringModeArg.equals("one-sided") ? ScoringMode.ONE_SIDED : ScoringMode.TWO_SIDED;
            Map<TermPair, Double> icMicaDict = icMicaData.icMicaDict();
            DifferentialDiagnosisEngine engine = new PhenomizerDifferentialDiagnosisEngine(hpoDiseases, icMicaDict, scoringMode);

            // Configure maxodiff refiner
            String refinerName = "MaxoDiff";
            DiffDiagRefiner refiner = maxodiffPropsConfiguration.diffDiagRefiner("score");

            // HPO : MAxO term map
            Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap = maxodiffPropsConfiguration.maxoAnnotsMap();

            try (BufferedWriter writer = openWriter(outputName); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
                printer.printRecord("phenopacket", "all_sample_ids", "n_sample_ids", "n_diseases", "n_repetitions",
                        "maxo_id", "maxo_label", "maxo_final_score", "maxo_final_random_spike_original_diagnosis",
                        "spiked_diagnosis", "spiked_diagnosis_idx",
                        "n_all_maxo_hpo_ids", "top_maxo_hpo_ids", "n_top_maxo_hpo_ids", "mean_n_disc_phen", "diff",
                        "refiner_type"); // header


                Set<TermId> allMaxoTerms = MaxoHpoTermIdMaps.getMaxoToHpoTermIdMap(hpoToMaxoTermMap).keySet();
                Set<TermId> allMaxoAscertainablePhenotypes = new HashSet<>();
                //7192 phenotypes discoverable by 313 MAxO terms
                int nAllMaxoDiscoverablePhenotypes = 1;
                double meanNDiscoverablePhenotypesAllMaxoTerms = 1;
                boolean computeMaxoAscertainablePhenotypes = true;
                Path allMaxoAscPhenPath = getAllMaxoAscPhenPath("allMaxoAscPhenotypes");

                // If file with all HPO terms ascertainable by all MAxO terms exists
                // calculate mean N discoverable phenotypes for all MAxO terms from file header
                if (Files.exists(allMaxoAscPhenPath)) {
                    computeMaxoAscertainablePhenotypes = false;
                    List<Double> nDiscPhens = calculateNDiscPhenotypesFromFile(allMaxoAscPhenPath);
                    nAllMaxoDiscoverablePhenotypes = (int) nDiscPhens.get(0).doubleValue();
                    meanNDiscoverablePhenotypesAllMaxoTerms = nDiscPhens.get(1);
                }

                int p = 1;
                int nPhenopackets = Math.min(phenopacketPaths.size(), 600);
                for (Path pPath0 : phenopacketPaths.subList(0, nPhenopackets)) {
                    String phenopacketName0 = pPath0.toFile().getName();
                    String outputFilename0 = String.join("_", phenopacketName0, ddEngine,
                                                String.join("", "n", nDiseasesList.getLast().toString()),
                                                String.join("","nr", nRepetitionsList.getLast().toString()),
                                                "maxodiff", "results.html");
                    Path maxodiffResultsHTMLPath0 = Path.of(String.join(File.separator, outputDir.toString(), outputFilename0));

                    // Skip phenopacket if results file already exists
                    if (Files.exists(maxodiffResultsHTMLPath0)) {
                        System.out.println("File " + outputFilename0 + " exists.");
                        continue;
                    }

                    try {
                        // Read phenopacket data and make sample
                        PhenopacketData phenopacketData = PhenopacketData.readPhenopacketData(pPath0);
                        Sample sample = Sample.of(phenopacketData.sampleId(),
                                phenopacketData.observedHpoTermIds().toList(),
                                phenopacketData.excludedHpoTermIds().toList());

                        LOGGER.info(String.valueOf(pPath0));
                        LOGGER.info("nDiseases = {}", nDiseasesList);

                        String phenopacketName = pPath0.toFile().getName();
                        List<TermId> allSampleHpoTerms = Stream.of(sample.observedHpoTermIds(), sample.excludedHpoTermIds())
                                .flatMap(Collection::stream).toList();

                        // Get initial differential diagnoses
                        List<DifferentialDiagnosis> differentialDiagnoses = engine.run(sample);
                        List<DifferentialDiagnosis> allOrderedDiagnoses = differentialDiagnoses.stream()
                                .sorted(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed())
                                .toList();

                        // Summarize the initial differential diagnosis results.
                        String outFilename = String.join("_",
                                phenopacketName.replace(".json", ""),
                                "initial",
                                ddEngine,
                                "results");
                        String ddOutputPath = String.join(File.separator, outputDir.toString(), outFilename + ".csv");
                        writeDifferentialDiagnosisResults(phenopacketName, allOrderedDiagnoses, Path.of(ddOutputPath));

                        // Use Ranked probability model
                        DiseaseModelProbability diseaseModelProbability = DiseaseModelProbability.ranked(allOrderedDiagnoses);

                        // If needed, make a file with all HPO terms ascertainable by all MAxO terms
                        // and calculate mean N discoverable phenotypes for all MAxO terms
                        if (computeMaxoAscertainablePhenotypes) {
                            computeAllMaxoAscertainablePhenotypes(hpoDiseases, hpoToMaxoTermMap,
                                    allOrderedDiagnoses, diseaseModelProbability,
                                    refiner, allMaxoTerms,
                                    sample, allMaxoAscertainablePhenotypes);
                            computeMaxoAscertainablePhenotypes = false;
                            List<Double> nDiscPhens = calculateNDiscPhenotypesFromFile(allMaxoAscPhenPath);
                            nAllMaxoDiscoverablePhenotypes = (int) nDiscPhens.get(0).doubleValue();
                            meanNDiscoverablePhenotypesAllMaxoTerms = nDiscPhens.get(1);
                        }


                        for (int nDiseases : nDiseasesList) {
                            // Get n diseases subset of initial diagnoses
                            List<DifferentialDiagnosis> initialDiagnosesNDiseases = allOrderedDiagnoses.subList(0, nDiseases);

                            // MaxoHpoTermProbabilities object
                            MaxoHpoTermProbabilities maxoHpoTermProbabilities = new MaxoHpoTermProbabilities(hpoDiseases,
                                    hpoToMaxoTermMap,
                                    initialDiagnosesNDiseases,
                                    diseaseModelProbability);

                            for (int nRepetitions : nRepetitionsList) {
                                // Make refinement options
                                RefinementOptions options = RefinementOptions.of(nDiseases, nRepetitions);
                                LOGGER.info("{}: {}", refinerName, refiner);
                                LOGGER.info("ppkt = {}, n Diseases = {}, n Repetitions = {}", phenopacketName, nDiseases, nRepetitions);

                                List<HpoDisease> diseases = refiner.getDiseases(initialDiagnosesNDiseases);
                                Map<TermId, List<HpoFrequency>> hpoTermCounts = refiner.getHpoTermCounts(diseases);
                                Map<TermId, Set<TermId>> maxoToHpoTermIdMap = refiner.getMaxoToHpoTermIdMap(hpoTermCounts);

                                DifferentialDiagnosisEngine diseaseSubsetEngine = engine;

                                RefinementResults refinementResults = getValidationResults(hpoToMaxoTermMap,
                                        maxoToHpoTermIdMap, maxoHpoTermProbabilities,
                                        diseaseSubsetEngine, ontology, allOrderedDiagnoses, refiner, sample,
                                        initialDiagnosesNDiseases, options, hpoTermCounts);

                                // Sort refinement results and write to files
                                List<MaxodiffResult> resultsList = new ArrayList<>(refinementResults.maxodiffResults().stream().toList());
                                resultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.rankMaxoScore().maxoScore()).reversed());

                                List<DifferentialDiagnosis> allInitialDiagnosesCopy = new ArrayList<>(allOrderedDiagnoses);
                                Collections.shuffle(allInitialDiagnosesCopy);
                                List<DifferentialDiagnosis> initialDiagnosesNDiseasesRandom = allInitialDiagnosesCopy.subList(0, options.nDiseases());

                                for (int d=0; d<initialDiagnosesNDiseases.size(); d++) {
                                    DifferentialDiagnosis originalDiagnosis = initialDiagnosesNDiseases.get(d);
                                    LOGGER.info("spike disease " + (d+1));
                                    LOGGER.info(originalDiagnosis.diseaseId().toString());
                                    String newFileName = "random_spike_disease_" + (d+1) + ".json";
                                    boolean writeOriginalJson = false;
                                    if (d == 0) {
                                        writeOriginalJson = true;
                                    }
                                    List<DifferentialDiagnosis> spikedNDiagnoses = new ArrayList<>(initialDiagnosesNDiseasesRandom);
                                    spikedNDiagnoses.set(spikedNDiagnoses.size()-1, originalDiagnosis);
                                    LOGGER.info(spikedNDiagnoses.toString());
                                    RefinementResults refinementResultsRandom = getValidationResults(hpoToMaxoTermMap,
                                            maxoToHpoTermIdMap, maxoHpoTermProbabilities,
                                            diseaseSubsetEngine, ontology, allOrderedDiagnoses, refiner, sample,
                                            spikedNDiagnoses, options, hpoTermCounts);
                                    LOGGER.info(refinementResultsRandom.maxodiffResults().stream().findFirst().get().rankMaxoScore().initialOmimTermIds().toString());



                                    writeValidationCSVResults(phenopacketName, refinerName, options, refinementResults,
                                            refinementResultsRandom, writeOriginalJson, originalDiagnosis.diseaseId(),
                                            d+1, newFileName,
                                            resultsList, biometadataService, meanNDiscoverablePhenotypesAllMaxoTerms,
                                            nAllMaxoDiscoverablePhenotypes, allSampleHpoTerms, printer);

                                }

//                                writeValidationHTMLResults(phenopacketName, options, ddEngine, sample, hpoDiseases,
//                                        biometadataService, hpoTermCounts, icMicaDict, resultsList);
                            }
                        }

                        float percent = (((float) p) / nPhenopackets) * 100;
                        LOGGER.info("Finished benchmark for {} ({} of {}. {}% complete)", phenopacketName, p, nPhenopackets, percent);
                        p++;
                    } catch (Exception ex) {
                        LOGGER.info(ex.getMessage());
                    }
                }

            }
            LOGGER.info("Finished benchmark.");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        return 0;
    }

    private RefinementResults getValidationResults(Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap,
                                      Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                                      MaxoHpoTermProbabilities maxoHpoTermProbabilities,
                                      DifferentialDiagnosisEngine diseaseSubsetEngine,
                                      Ontology ontology,
                                      List<DifferentialDiagnosis> allOrderedDiagnoses,
                                      DiffDiagRefiner refiner,
                                      Sample sample,
                                      List<DifferentialDiagnosis> initialDiagnosesNDiseases,
                                      RefinementOptions options,
                                      Map<TermId, List<HpoFrequency>> hpoTermCounts) throws Exception {

        // Perform maxodiff refinement
        RankMaxo rankMaxo = new RankMaxo(hpoToMaxoTermMap, maxoToHpoTermIdMap,
                maxoHpoTermProbabilities, diseaseSubsetEngine,
                ontology, allOrderedDiagnoses, initialDiagnosesNDiseases);

        RefinementResults refinementResults = refiner.run(sample,
                initialDiagnosesNDiseases,
                options,
                rankMaxo,
                hpoTermCounts,
                maxoToHpoTermIdMap);

        return refinementResults;
    }

    private void writeValidationCSVResults(String phenopacketName,
                                           String refinerName,
                                           RefinementOptions options,
                                           RefinementResults refinementResults,
                                           RefinementResults refinementResultsRandom,
                                           boolean writeOriginalJson,
                                           TermId originalDiseaseId,
                                           int originalDiseaseIdx,
                                           String newName,
                                           List<MaxodiffResult> resultsList,
                                           BiometadataService biometadataService,
                                           double meanNDiscoverablePhenotypesAllMaxoTerms,
                                           int nAllMaxoDiscoverablePhenotypes,
                                           List<TermId> allSampleHpoTerms,
                                           CSVPrinter printer) throws IOException {


        if (writeOriginalJson) {
            String fileName = String.join("_",
                    phenopacketName.replace(".json", ""),
                    "n" + options.nDiseases(),
                    "nr" + options.nRepetitions(),
                    refinerName + ".json");
            Path maxodiffResultsFilePath = Path.of(String.join(File.separator, outputDir.toString(), fileName));
            writeToJsonFile(maxodiffResultsFilePath, refinementResults);
        }


        String fileNameRandom = String.join("_",
                phenopacketName.replace(".json", ""),
                "n" + options.nDiseases(),
                "nr" + options.nRepetitions(),
                newName);
        Path maxodiffResultsFilePathRandom = Path.of(String.join(File.separator, outputDir.toString(), fileNameRandom));
        writeToJsonFile(maxodiffResultsFilePathRandom, refinementResultsRandom);

        // Test new validation procedure
        // Get highest score MAxO term id
        MaxodiffResult topResult = resultsList.getFirst();
        TermId topMaxoId = topResult.rankMaxoScore().maxoId();

        String maxScoreTermLabel = biometadataService.maxoLabel(topMaxoId.toString()).orElse("unknown");
        double maxScoreValue = topResult.rankMaxoScore().maxoScore(); //maxoTermScore().scoreDiff();

        MaxodiffResult topResultRandom = refinementResultsRandom.maxodiffResults().stream()
                .filter(mr -> mr.rankMaxoScore().maxoId().equals(topMaxoId)).toList().getFirst();
        double maxScoreValueRandom = topResultRandom.rankMaxoScore().maxoScore();

        LOGGER.info("{}: n Diseases = {}, n Repetitions = {}", refinerName, options.nDiseases(), options.nRepetitions());

        LOGGER.info("Max Score: {} ({}) = {}", topMaxoId, maxScoreTermLabel, maxScoreValue);

        LOGGER.info("Getting Top Maxo Ascertainable Phenotypes...");

        // Get top MAxO result top ascertainable phenotypes
        Set<TermId> topMaxoAscertainablePhenotypes = topResult.rankMaxoScore().discoverableObservedHpoTermIds();//maxoHpoTermProbabilities.getDiscoverableByMaxoHpoTerms(sample, topMaxoId, maxoToHpoTermIdMap);

        double diff = topMaxoAscertainablePhenotypes.size() - meanNDiscoverablePhenotypesAllMaxoTerms;

        // Write Benchmark results summary file
        writeResults(phenopacketName, allSampleHpoTerms, allSampleHpoTerms.size(), options.nDiseases(),
                options.nRepetitions(), topMaxoId.toString(), maxScoreTermLabel, maxScoreValue, maxScoreValueRandom,
                originalDiseaseId, originalDiseaseIdx, nAllMaxoDiscoverablePhenotypes,
                topMaxoAscertainablePhenotypes, topMaxoAscertainablePhenotypes.size(),
                meanNDiscoverablePhenotypesAllMaxoTerms, diff,
                refinerName, printer);
    }

    private void writeValidationHTMLResults(String phenopacketName,
                                            RefinementOptions options,
                                            String ddEngine,
                                            Sample sample,
                                            HpoDiseases hpoDiseases,
                                            BiometadataService biometadataService,
                                            Map<TermId, List<HpoFrequency>> hpoTermCounts,
                                            Map<TermPair, Double> icMicaDict,
                                            List<MaxodiffResult> resultsList) throws Exception {

        // Write HTML results
        String nDiseasesAbbr = String.join("", "n", String.valueOf(options.nDiseases()));
        String nRepsAbbr = String.join("", "nr", String.valueOf(options.nRepetitions()));
        String outputFilename = String.join("_", phenopacketName, ddEngine,
                nDiseasesAbbr, nRepsAbbr, "maxodiff", "results.html");
        Path maxodiffResultsHTMLPath = Path.of(String.join(File.separator, outputDir.toString(), outputFilename));

        String htmlString = HtmlResults.writeHTMLResults(sample, options.nDiseases(), hpoDiseases, options.nRepetitions(), resultsList,
                biometadataService, hpoTermCounts, icMicaDict);

        Files.writeString(maxodiffResultsHTMLPath, htmlString);
    }

    private List<Double> calculateNDiscPhenotypesFromFile(Path allMaxoAscPhenPath) throws IOException {
        double nAllMaxoDiscoverablePhenotypes = 1;
        double meanNDiscoverablePhenotypesAllMaxoTerms = 1;
        File allAscPhenFile = allMaxoAscPhenPath.toFile();
        List<Integer> integers = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(allAscPhenFile))) {
            String headerLine = reader.readLine(); // Read the first line
            Pattern integerPattern = Pattern.compile("\\d+");
            Matcher integerMatcher = integerPattern.matcher(headerLine);
            while (integerMatcher.find()) {
                integers.add(Integer.parseInt(integerMatcher.group()));
            }
            double nAllMaxoTerms = integers.get(1); //313;//257;
            nAllMaxoDiscoverablePhenotypes = integers.get(0); //7192;//7036;//6170;//5302;
            meanNDiscoverablePhenotypesAllMaxoTerms = nAllMaxoTerms / nAllMaxoDiscoverablePhenotypes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return List.of(nAllMaxoDiscoverablePhenotypes, meanNDiscoverablePhenotypesAllMaxoTerms);
    }

    private Path getAllMaxoAscPhenPath(String pathNameAddition) {
        String outputFileNameStr = outputName.getFileName().toString();
        String allMaxoAscPhenFileName = outputFileNameStr.replace(".csv", "_" + pathNameAddition + ".csv");
        String outPath = outputName.toAbsolutePath().toString();
        String allMaxoAscPhenPathStr = outPath.replace(outputFileNameStr, allMaxoAscPhenFileName);
        return Path.of(allMaxoAscPhenPathStr);
    }


    public void writeToJsonFile(Path filePath, RefinementResults results) throws IOException {
        ObjectWriter writer = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();
        writer.writeValue(new File(filePath.toString()), results);
    }


    private static BufferedWriter openWriter(Path outputPath) throws IOException {
        return outputPath.toFile().getName().endsWith(".gz")
                ? new BufferedWriter(new OutputStreamWriter(
                        new GZIPOutputStream(Files.newOutputStream(outputPath, StandardOpenOption.APPEND,
                                StandardOpenOption.CREATE, StandardOpenOption.WRITE))))
                : Files.newBufferedWriter(outputPath, StandardOpenOption.APPEND,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    /**
     * Write results of a single benchmark into the provided {@code printer}.
     */
    private static void writeResults(String phenopacketName,
                                     List<TermId> sampleIds,
                                     int nSampleIds,
                                     int nDiseases,
                                     int nRepetitions,
                                     String maxoId,
                                     String maxoLabel,
                                     double maxoFinalScore,
                                     double maxoFinalScoreRandom,
                                     TermId originalDiseaseId,
                                     int originalDiseaseIdx,
                                     int nAllMaxoHpoTerms,
                                     Set<TermId> topMaxoHpoTerms,
                                     int nTopMaxoHpoTerms,
                                     double meanNDiscPhenotypes,
                                     double diff,
                                     String refinerType,
                                     CSVPrinter printer) {

        try {
            printer.print(phenopacketName);
            printer.print(sampleIds);
            printer.print(nSampleIds);
            printer.print(nDiseases);
            printer.print(nRepetitions);
            printer.print(maxoId);
            printer.print(maxoLabel);
            printer.print(maxoFinalScore);
            printer.print(maxoFinalScoreRandom);
            printer.print(originalDiseaseId);
            printer.print(originalDiseaseIdx);
            printer.print(nAllMaxoHpoTerms);
            printer.print(topMaxoHpoTerms);
            printer.print(nTopMaxoHpoTerms);
            printer.print(meanNDiscPhenotypes);
            printer.print(diff);
            printer.print(refinerType);
            printer.println();
        } catch (IOException e) {
            LOGGER.error("Error writing results for {}: {}", phenopacketName, e.getMessage(), e);
        }
    }

    /**
     * Write results of a differential diagnosis into the provided {@code printer}.
     */
    private static void writeDifferentialDiagnosisResults(String phenopacketName,
                                     List<DifferentialDiagnosis> ddList,
                                     Path outputName) throws IOException {

        try (BufferedWriter writer = openWriter(outputName); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord("disease_id", "posttest_prob", "lr"); // header
            for (DifferentialDiagnosis dd : ddList) {
                printer.print(dd.diseaseId());
                printer.print(dd.score());
                printer.print(dd.lr());
                printer.println();
            }
        } catch (IOException e) {
            LOGGER.error("Error writing differential diagnosis results for {}: {}", phenopacketName, e.getMessage(), e);
        }
    }

    /**
     * Write all HPO Phenotypes ascertainable by all MAxO terms into the provided {@code printer}.
     */
    private static void writeAllMaxoAscPhenotypes(Set<TermId> allMaxoAscPhenotypes,
                                                  int nMaxoTerms,
                                                  Path outputName) {

        try (BufferedWriter writer = openWriter(outputName); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            String header = allMaxoAscPhenotypes.size() + " HPO phenotypes ascertainable by " + nMaxoTerms + " MAxO terms.";
            printer.print(header);
            printer.println();
            for (TermId hpoId : allMaxoAscPhenotypes) {
                printer.print(hpoId);
                printer.println();
            }
            LOGGER.info("Wrote all MAxO ascertainable phenotypes to " + outputName);
        } catch (IOException e) {
            LOGGER.error("Error writing all MAxO ascertainable phenotype results: {}", e.getMessage(), e);
        }
    }

    private void computeAllMaxoAscertainablePhenotypes(HpoDiseases hpoDiseases,
                                                       Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap,
                                                       List<DifferentialDiagnosis> differentialDiagnoses,
                                                       DiseaseModelProbability diseaseModelProbability,
                                                       DiffDiagRefiner refiner,
                                                       Set<TermId> allMaxoTerms,
                                                       Sample sample,
                                                       Set<TermId> allMaxoAscertainablePhenotypes) {

        LOGGER.info("Getting All Maxo Ascertainable Phenotypes...");
        MaxoHpoTermProbabilities allMaxoHpoTermProbabilities = new MaxoHpoTermProbabilities(hpoDiseases,
                hpoToMaxoTermMap,
                differentialDiagnoses,
                diseaseModelProbability);

        List<DifferentialDiagnosis> orderedDiagnoses = differentialDiagnoses.stream()
                .sorted(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed())
                .toList();
        List<HpoDisease> diseases = refiner.getDiseases(orderedDiagnoses);
        Map<TermId, List<HpoFrequency>> hpoTermCounts = refiner.getHpoTermCounts(diseases);
        Map<TermId, Set<TermId>> fullMaxoToHpoTermIdMap = refiner.getMaxoToHpoTermIdMap(hpoTermCounts);

        int m = 1;
        int nMaxoTerms = allMaxoTerms.size();
        long start = System.currentTimeMillis();
        for (TermId maxoId : allMaxoTerms) {
            LOGGER.info(maxoId.toString());
            Set<TermId> maxoAscertainablePhenotypes = allMaxoHpoTermProbabilities.getDiscoverableByMaxoHpoTerms(sample, maxoId, fullMaxoToHpoTermIdMap);
            allMaxoAscertainablePhenotypes.addAll(maxoAscertainablePhenotypes);
            float percent = (((float) m) / nMaxoTerms) * 100;
            LOGGER.info("Finished {} of {} MAxO terms. {}% complete.", m, nMaxoTerms, percent);
            m++;
        }
        long end = System.currentTimeMillis();
        long allMaxoAscertainablePhenoypesCalcTime = (end - start) / 1000;

        Path allMaxoAscPhenPath = getAllMaxoAscPhenPath("allMaxoAscPhenotypes");

        writeAllMaxoAscPhenotypes(allMaxoAscertainablePhenotypes, nMaxoTerms, allMaxoAscPhenPath);

        long HH = allMaxoAscertainablePhenoypesCalcTime / 3600;
        long MM = (allMaxoAscertainablePhenoypesCalcTime % 3600) / 60;
        long SS = allMaxoAscertainablePhenoypesCalcTime % 60;
        String timeInHHMMSS = String.format("%02d:%02d:%02d", HH, MM, SS);
        LOGGER.info("All MAxO Ascertainable Phenotypes Calculated in " + timeInHHMMSS);
    }
}
