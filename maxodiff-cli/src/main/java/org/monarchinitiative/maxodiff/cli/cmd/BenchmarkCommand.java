package org.monarchinitiative.maxodiff.cli.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.config.MaxodiffPropsConfiguration;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.io.PhenopacketFileParser;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.lirical.LiricalDifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.lirical.LiricalDifferentialDiagnosisEngineConfigurer;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
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

    @CommandLine.Option(names = {"-W", "--weights"},
            split=",",
            arity = "1..*",
            description = "Comma-separated list of weight values to use in final score calculation.")
    public List<Double> weightsArg;

    @CommandLine.Option(names = {"-N", "--nDiseasesList"},
//            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of posttest probability thresholds for filtering diseases to include in differential diagnosis.")
    protected List<Integer> nDiseasesArg;

    @CommandLine.Option(names = {"--dummy"},
            description = "Whether to do dummy refinement (default: ${DEFAULT-VALUE}).")
    protected boolean dummy = false;

    @CommandLine.Option(names = {"-R", "--removeIdsFile"},
            description = "Path to file containing term Ids to remove for each phenopacket.")
    protected String removeIdsFile;

    @CommandLine.Option(names = {"--removeSampleTerms"},
            description = "Whether to remove Sample HPO terms before analysis (default: ${DEFAULT-VALUE}).")
    protected boolean removeSampleTerms = false;

    @CommandLine.Option(names = {"-o", "--outputFilename"},
            description = "Filename of the benchmark results CSV file. The CSV is compressed if the path has the '.gz' suffix")
    protected Path outputName;

    @CommandLine.Option(names = {"--refinerTypes"},
//            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of differential diagnosis refiners.")
    protected List<String> refinerTypes;

//    private final BiometadataService biometadataService;
//
//    private final DiffDiagRefiner diffDiagRefiner;

//    public BenchmarkCommand(
//            BiometadataService biometadataService,
//            DiffDiagRefiner diffDiagRefiner) {
//        this.biometadataService = biometadataService;
//        this.diffDiagRefiner = diffDiagRefiner;
//    }


    @Override
    public Integer call() throws Exception {

        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        OBJECT_MAPPER.registerModule(new Jdk8Module());

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

        List<Double> weights = new ArrayList<>();
        weightsArg.stream().forEach(w -> weights.add(w));
        List<Integer> nDiseasesList = new ArrayList<>();
        nDiseasesArg.stream().forEach(n -> nDiseasesList.add(n));
        List<String> refinersList = new ArrayList<>();
        if (refinerTypes != null)
            refinerTypes.stream().forEach(r -> refinersList.add(r));

        Lirical lirical = bootstrapLirical();
        try (LiricalAnalysisRunner runner = lirical.analysisRunner()) {
            LiricalDifferentialDiagnosisEngineConfigurer configurer = LiricalDifferentialDiagnosisEngineConfigurer.of(runner);
            AnalysisOptions analysisOptions = prepareAnalysisOptions(lirical);
            LiricalDifferentialDiagnosisEngine engine = configurer.configure(analysisOptions);

            // Make maxodiffRefiner
            MaxodiffDataResolver maxodiffDataResolver = MaxodiffDataResolver.of(maxoDataPath);
            MaxodiffPropsConfiguration maxodiffPropsConfiguration = MaxodiffPropsConfiguration.createConfig(maxodiffDataResolver);

            Map<String, DiffDiagRefiner> refiners = new HashMap<>();
            refiners.put("MaxoDiff", maxodiffPropsConfiguration.diffDiagRefiner("score"));
            for (String refiner : refinersList) {
                refiners.put(refiner, maxodiffPropsConfiguration.diffDiagRefiner(refiner));
            }

            BiometadataService biometadataService = maxodiffPropsConfiguration.biometadataService();

            try (BufferedWriter writer = openWriter(outputName); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
                printer.printRecord("phenopacket", "removed_ids", "n_diseases", "weight",
                        "maxo_id", "maxo_label", "maxo_final_score", "changed_disease_id",
                        "orig_disease_rank", "maxo_disease_rank", "orig_disease_score", "maxo_disease_score", "refiner_type"); // header

                for (int i = 0; i < phenopacketPaths.size(); i++) {
                    try {

                        Path pPath = phenopacketPaths.get(i);
                        PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(pPath);
                        Sample sample = Sample.of(phenopacketData.sampleId(),
                                phenopacketData.presentHpoTermIds().toList(),
                                phenopacketData.excludedHpoTermIds().toList());

                        LOGGER.info(String.valueOf(phenopacketPath));
                        LOGGER.info("weights = " + weights);
                        LOGGER.info("nDiseases = " + nDiseasesList);
                        LOGGER.info("refiners = " + refinersList);

                        // Get initial differential diagnoses from running LIRICAL
                        List<DifferentialDiagnosis> differentialDiagnoses = engine.run(sample);

                        // Summarize the LIRICAL results.
                        //String sampleId = analysisData.sampleId();
                        String phenopacketName = pPath.toFile().getName();
                        //String outFilename = String.join("_",
                        //        phenopacketName.replace(".json", ""),
                        //        "lirical",
                        //        "results");
                        //AnalysisResultsMetadata metadata = prepareAnalysisResultsMetadata(gene2Genotypes, lirical, sampleId);
                        //writeResultsToFile(lirical, OutputFormat.parse(outputFormatArg), analysisData, results, metadata, outFilename);

                        //TODO? get list of diseases from LIRICAL results, and add diseases from CLI arg to total list for analysis

                        List<TermId> termIdsToRemove = new ArrayList<>();
                        if (removeIdsFile != null) {
                            termIdsToRemove = getTermIdsToRemove(phenopacketName, removeIdsFile);
                        }
                        if (removeSampleTerms) {
                            termIdsToRemove = Stream.of(sample.presentHpoTermIds(), sample.excludedHpoTermIds())
                                    .flatMap(Collection::stream).toList();
                        }
                        LOGGER.info(phenopacketName + " removed Ids = " + termIdsToRemove.toString());

                        Map<Integer, Map<TermId, List<DifferentialDiagnosis>>> nDiseaseMaxoTermToDifferentialDiagnosesMap = new HashMap<>();
                        for (Map.Entry<String, DiffDiagRefiner> e : refiners.entrySet()) {
                            for (int nDiseases : nDiseasesList) {

                                // Make MaXo:HPO Term Map
                                //                Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoTermMap = maxoTermMap.makeMaxoToHpoTermMap(results, null,
                                //                        phenopacketPath, nDiseases);
                                //
                                //                LOGGER.info(String.valueOf(maxoToHpoTermMap));
                                for (double weight : weights) {
                                    RefinementOptions options = RefinementOptions.of(nDiseases, weight);
                                    LOGGER.info(e.getKey() + ": " + e.getValue());
                                    LOGGER.info("n Diseases = " + nDiseases + ", Weight = " + weight);
                                    List<DifferentialDiagnosis> orderedDiagnoses = e.getValue().getOrderedDiagnoses(differentialDiagnoses, options);
                                    List<HpoDisease> diseases = e.getValue().getDiseases(orderedDiagnoses);
                                    Map<TermId, List<HpoFrequency>> hpoTermCounts = e.getValue().getHpoTermCounts(diseases);
                                    Map<TermId, Set<TermId>> maxoToHpoTermIdMap = e.getValue().getMaxoToHpoTermIdMap(termIdsToRemove, hpoTermCounts);
                                    Map<TermId, List<DifferentialDiagnosis>> maxoTermToDifferentialDiagnosesMap = null;
                                    if (e.getValue() instanceof MaxoDiffDDScoreRefiner | e.getValue() instanceof MaxoDiffRankRefiner) {
                                        Integer nMapDiseases = options.nDiseases();
                                        if (!nDiseaseMaxoTermToDifferentialDiagnosesMap.containsKey(nMapDiseases)) {
                                            maxoTermToDifferentialDiagnosesMap = e.getValue()
                                                    .getMaxoTermToDifferentialDiagnosesMap(sample, engine, maxoToHpoTermIdMap, nMapDiseases);
                                            nDiseaseMaxoTermToDifferentialDiagnosesMap.put(nMapDiseases, maxoTermToDifferentialDiagnosesMap);
                                        }
                                        maxoTermToDifferentialDiagnosesMap = nDiseaseMaxoTermToDifferentialDiagnosesMap.get(nMapDiseases);
                                    } else if (e.getValue() instanceof MaxoDiffKolmogorovSmirnovRefiner) {
                                        Integer nMapDiseases = 100;
                                        maxoTermToDifferentialDiagnosesMap = e.getValue()
                                                .getMaxoTermToDifferentialDiagnosesMap(sample, engine, maxoToHpoTermIdMap, nMapDiseases);
                                    }
                                    RefinementResults refinementResults = e.getValue().run(sample, orderedDiagnoses, options, engine,
                                            maxoToHpoTermIdMap, hpoTermCounts, maxoTermToDifferentialDiagnosesMap);
                                    List<MaxodiffResult> resultsList = refinementResults.maxodiffResults().stream().toList();
                                    // Get List of Refinement results: maxo term scores and frequencies
                                    String fileName = String.join("_",
                                            phenopacketName.replace(".json", ""),
                                            "n" + nDiseases,
                                            "w" + weight,
                                            e.getKey() + ".json");
                                    Path maxodiffResultsFilePath = Path.of(String.join(File.separator, outputDir.toString(), fileName));
                                    writeToJsonFile(maxodiffResultsFilePath, refinementResults);
                                    // Take the MaXo term that has the highest score
                                    MaxodiffResult topResult = resultsList.get(0);
                                    String maxScoreMaxoTermId = topResult.maxoTermScore().maxoId();
                                    String maxScoreTermLabel = biometadataService.maxoLabel(maxScoreMaxoTermId).orElse("unknown");
                                    double maxScoreValue = topResult.maxoTermScore().scoreDiff();
                                    TermId changedDiseaseId;
                                    int origRank = 0;
                                    double origLR = 0;
                                    int maxoRank = 0;
                                    double maxoLR = 0;
                                    if (e.getValue() instanceof MaxoDiffDDScoreRefiner | e.getValue() instanceof MaxoDiffRankRefiner) {
                                        changedDiseaseId = topResult.maxoTermScore().changedDiseaseId();
                                        Map<String, List<DifferentialDiagnosis>> calculatedDiagnosesMap = new HashMap<>();
                                        calculatedDiagnosesMap.put("Original", topResult.maxoTermScore().initialDiagnosesMaxoOrdered());
                                        calculatedDiagnosesMap.put("Maxo", topResult.maxoTermScore().maxoDiagnoses());
                                        for (Map.Entry<String, List<DifferentialDiagnosis>> entry : calculatedDiagnosesMap.entrySet()) {
                                            List<DifferentialDiagnosis> calculatedDiagnoses = entry.getValue();
                                            List<DifferentialDiagnosis> changedDiseaseDiagnosisList = calculatedDiagnoses
                                                    .stream().filter(dd -> dd.diseaseId().equals(changedDiseaseId)).toList();
                                            if (!changedDiseaseDiagnosisList.isEmpty()) {
                                                DifferentialDiagnosis changedDiseaseDiagnosis = changedDiseaseDiagnosisList.get(0);
                                                if (entry.getKey().equals("Original")) {
                                                    origRank = calculatedDiagnoses.indexOf(changedDiseaseDiagnosis)+1;
                                                    origLR = changedDiseaseDiagnosis.lr();
                                                } else {
                                                    maxoRank = calculatedDiagnoses.indexOf(changedDiseaseDiagnosis)+1;
                                                    maxoLR = changedDiseaseDiagnosis.lr();
                                                }
                                            }
                                        }
                                    } else {
                                        changedDiseaseId = null;
                                    }

                                    LOGGER.info(e.getKey() + ": n Diseases = " + nDiseases + ", Weight = " + weight);

                                    LOGGER.info("Max Score: " + maxScoreMaxoTermId + " (" + maxScoreTermLabel + ")" + " = " + maxScoreValue);
                                    writeResults(phenopacketName, termIdsToRemove, nDiseases, weight,
                                            maxScoreMaxoTermId, maxScoreTermLabel, maxScoreValue, changedDiseaseId,
                                            origRank, maxoRank, origLR, maxoLR, e.getKey(), printer);

                                    if (e.getKey().equals("rank") | e.getKey().equals("ddScore") | e.getKey().equals("ksTest")) {
                                        break;
                                    }
                                }

                                TermId diseaseId = phenopacketData.diseaseIds().get(0);

                                if (e.getKey().equals("ksTest")) {
                                    break;
                                }
                            }
                        }
                        LOGGER.info("Finished benchmark for " + phenopacketName);
                    } catch (Exception ex) {
                        LOGGER.info(ex.getMessage());
                    }
                }
            }
            LOGGER.info("Finished benchmark.");
        }

        return 0;
    }


    public void writeToJsonFile(Path filePath, RefinementResults results) throws IOException {
        ObjectWriter writer = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();
        writer.writeValue(new File(filePath.toString()), results);
    }

    public List<TermId> getTermIdsToRemove(String phenopacketName, String removeIdsMapFile) {
        List<TermId> termIdsToRemove = new ArrayList<>();
        File removeFile = new File(removeIdsMapFile);
        try (BufferedReader br = new BufferedReader(new FileReader(removeFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) continue;
                try {
                    String [] fields = line.split("\t");
                    String pName = fields[0];
                    if (pName.equals(phenopacketName)) {
                        termIdsToRemove = Arrays.stream(fields[1].split(",")).map(TermId::of).toList();
                    }
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        return termIdsToRemove;
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
                                     List<TermId> removedIds,
                                     int nDiseases,
                                     double weight,
                                     String maxoId,
                                     String maxoLabel,
                                     double maxoFinalScore,
                                     TermId changedDiseaseId,
                                     int origRank,
                                     int maxoRank,
                                     double origScore,
                                     double maxoScore,
                                     String refinerType,
                                     CSVPrinter printer) {

        try {
            printer.print(phenopacketName);
            printer.print(removedIds);
            printer.print(nDiseases);
            printer.print(weight);
            printer.print(maxoId);
            printer.print(maxoLabel);
            printer.print(maxoFinalScore);
            printer.print(changedDiseaseId);
            printer.print(origRank);
            printer.print(maxoRank);
            printer.print(origScore);
            printer.print(maxoScore);
            printer.print(refinerType);
            printer.println();
        } catch (IOException e) {
            LOGGER.error("Error writing results for {}: {}", phenopacketName, e.getMessage(), e);
        }
    }


}
