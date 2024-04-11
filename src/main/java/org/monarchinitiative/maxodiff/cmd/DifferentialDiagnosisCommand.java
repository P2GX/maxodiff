package org.monarchinitiative.maxodiff.cmd;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.biodownload.FileDownloadException;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.lirical.core.io.VariantParser;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.LiricalVariant;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.lirical.core.output.AnalysisResultsMetadata;
import org.monarchinitiative.lirical.core.output.AnalysisResultsWriter;
import org.monarchinitiative.lirical.core.output.OutputFormat;
import org.monarchinitiative.lirical.core.output.OutputOptions;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporter;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporters;
import org.monarchinitiative.maxodiff.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.DiseaseTermCount;
import org.monarchinitiative.maxodiff.SimpleTerm;
import org.monarchinitiative.maxodiff.analysis.MaxoDiffVisualizer;
import org.monarchinitiative.maxodiff.analysis.MaxodiffAnalyzer;
import org.monarchinitiative.maxodiff.io.InputFileParser;
import org.monarchinitiative.maxodiff.io.MaxoDxAnnots;
import org.monarchinitiative.maxodiff.io.MaxodiffBuilder;
import org.monarchinitiative.maxodiff.io.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.service.MaxoDiffService;
import org.monarchinitiative.maxodiff.service.PhenotypeService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;


/**
 * Perform Differential Diagnosis calculations
 */

@CommandLine.Command(name = "diagnosis", aliases = {"d"},
        mixinStandardHelpOptions = true,
        description = "maxodiff analysis")
public class DifferentialDiagnosisCommand extends BaseLiricalCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DifferentialDiagnosisCommand.class);

    @CommandLine.Option(names = {"-p", "--phenopacket"},
//            required = true,
            arity = "1..*",
            description = "Path(s) to phenopacket JSON file(s).")
    protected List<Path> phenopacketPaths;

    @CommandLine.Option(names = {"-B", "--batchDir"},
            description = "Path to directory containing phenopackets.")
    protected String batchDir;

    @CommandLine.Option(names = {"--assembly"},
            paramLabel = "{hg19,hg38}",
            description = "Genome build (default: ${DEFAULT-VALUE}).")
    protected String genomeBuild = "hg38";

    @CommandLine.Option(names = {"--vcf"},
            description = "Path to VCF with background variants.")
    protected Path vcfPath; // nullable

    @CommandLine.Option(names = {"-O", "--outputDirectory"},
//            required = true,
            description = "Where to write the LIRICAL results files.")
    protected Path outputDir;

    @CommandLine.Option(names = {"--format"},
            paramLabel = "{tsv,html,json}",
            description = "LIRICAL results output format (default: ${DEFAULT-VALUE}).")
    protected String outputFormatArg = "tsv";

    @CommandLine.Option(names = {"--compress"},
            paramLabel = "{tsv,html,json}",
            description = "Whether to output LIRICAL results file as a compressed file (default: ${DEFAULT-VALUE}).")
    protected boolean compress = false;

    @CommandLine.Option(names = {"-w", "--weight"},
            split=",",
            arity = "1..*",
            description = "Weight value to use in final score calculation (default: ${DEFAULT-VALUE}).")
    public List<Double> weightsArg;

    @CommandLine.Option(names = {"-l", "--diseaseList"},
//            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of diseases to include in differential diagnosis.")
    protected List<String> diseaseIdsArg;

    @CommandLine.Option(names = {"-t", "--threshold"},
//            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of posttest probability thresholds for filtering diseases to include in differential diagnosis.")
    protected List<Double> thresholdArg;


    @Override
    public Integer call() throws Exception {

        if (batchDir != null) {
            phenopacketPaths = new ArrayList<>();
            File folder = new File(batchDir);
            File[] files = folder.listFiles();
            assert files != null;
            for (File file : files) {
                BasicFileAttributes basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                if (basicFileAttributes.isRegularFile() && !basicFileAttributes.isDirectory() && !file.getName().startsWith(".")) {
                    phenopacketPaths.add(file.toPath());
                }
            }
        }
        Collections.sort(phenopacketPaths);

        MaxodiffDataResolver dataResolver = new MaxodiffDataResolver(dataSection.liricalDataDirectory);
        MaxoDxAnnots maxoDxAnnots = new MaxoDxAnnots(dataResolver.maxoDxAnnots());
        Map<SimpleTerm, Set<SimpleTerm>> fullHpoToMaxoTermMap = maxoDxAnnots.getSimpleTermSetMap();

        List<Double> weights = new ArrayList<>();
        weightsArg.stream().forEach(w -> weights.add(w));
//        List<Integer> topNDiseases = Arrays.asList(5, 10, 15, 20);
        List<Double> filterPosttestProbs = new ArrayList<>(); //Arrays.asList(0.05, 0.1, 0.2, 0.4, 0.6, 0.8);
        thresholdArg.stream().forEach(t -> filterPosttestProbs.add(t));
        Path maxodiffResultsFilePath = Path.of(String.join(File.separator, outputDir.toString(), "maxodiff_results.csv"));

        try (BufferedWriter writer = openWriter(maxodiffResultsFilePath); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord("phenopacket", "background_vcf", "disease_id", "maxo_id", "maxo_label",
                    "filter_posttest_prob", "n_diseases", "disease_ids", "weight", "score"); // header

            for (int i = 0; i < phenopacketPaths.size(); i++) {
                try {
                    // Read phenopacket data.
                    Path phenopacketPath = phenopacketPaths.get(i);
                    PhenopacketData phenopacketData = readPhenopacketData(phenopacketPath);

                    // Prepare LIRICAL analysis options
                    Lirical lirical = prepareLirical();
                    AnalysisOptions analysisOptions = prepareAnalysisOptions(lirical);

                    // Read variants if present.
                    GenesAndGenotypes gene2Genotypes = readVariants(vcfPath, lirical, analysisOptions.genomeBuild());

                    // Prepare LIRICAL analysis data.
                    AnalysisData analysisData = AnalysisData.of(phenopacketData.sampleId(),
                            phenopacketData.parseAge().orElse(null),
                            phenopacketData.parseSex().orElse(Sex.UNKNOWN),
                            phenopacketData.presentHpoTermIds().toList(),
                            phenopacketData.excludedHpoTermIds().toList(),
                            gene2Genotypes);

                    // Run the LIRICAL analysis.
                    LOGGER.info("Starting the analysis: {}", analysisOptions);
                    AnalysisResults results;
                    try (LiricalAnalysisRunner analysisRunner = lirical.analysisRunner()) {
                        results = analysisRunner.run(analysisData, analysisOptions);
                    }

                    // Summarize the LIRICAL results.
                    String sampleId = analysisData.sampleId();
                    String phenopacketName = phenopacketPath.toFile().getName();
                    String outFilename = String.join("_",
                            phenopacketName.replace(".json", ""),
                            "lirical",
                            "results");
                    AnalysisResultsMetadata metadata = prepareAnalysisResultsMetadata(gene2Genotypes, lirical, sampleId);
                    //writeResultsToFile(lirical, OutputFormat.parse(outputFormatArg), analysisData, results, metadata, outFilename);

                    //TODO? get list of diseases from LIRICAL results, and add diseases from CLI arg to total list for analysis

                    for (double n : filterPosttestProbs) {
                        LOGGER.info("Min Posttest Probabiltiy Threshold = " + n);
                        // Collect HPO terms and frequencies for the target m diseases
                        List<TermId> diseaseIds = new ArrayList<>();
//            diseaseIdsArg.stream().forEach(id -> diseaseIds.add(TermId.of(id)));
//                        List<TestResult> testResults = results.resultsWithDescendingPostTestProbability().collect(Collectors.toList()).subList(0, n);
                        List<TestResult> testResults = results.resultsWithDescendingPostTestProbability()
                                .filter(r -> r.posttestProbability() >= n)
                                .collect(Collectors.toList());
                        testResults.forEach(r -> diseaseIds.add(r.diseaseId()));
                        LOGGER.info(phenopacketPath + " diseaseIds: " + String.valueOf(diseaseIds));
                        int topNDiseases = diseaseIds.size();

                        DifferentialDiagnosis diffDiag = new DifferentialDiagnosis();
                        List<HpoDisease> diseases = diffDiag.makeDiseaseList(dataResolver, diseaseIds);
                        DiseaseTermCount diseaseTermCount = DiseaseTermCount.of(diseases);
                        Map<TermId, List<Object>> hpoTermCounts = diseaseTermCount.hpoTermCounts();

                        // Remove HPO terms present in the phenopacket
                        phenopacketData.presentHpoTermIds().forEach(id -> hpoTermCounts.remove(id));
                        phenopacketData.excludedHpoTermIds().forEach(id -> hpoTermCounts.remove(id));

                        // Get all the MaXo terms that can be used to diagnose the HPO terms
                        Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap = diffDiag.makeHpoToMaxoTermMap(fullHpoToMaxoTermMap, hpoTermCounts.keySet());
                        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = diffDiag.makeMaxoToHpoTermIdMap(hpoToMaxoTermMap);

                        LOGGER.info(String.valueOf(maxoToHpoTermIdMap));

                        for (double weight : weights) {
                            LOGGER.info("Weight = " + weight);
                            // Make map of MaXo scores
                            Map<TermId, Double> maxoScoreMap = diffDiag.makeMaxoScoreMap(maxoToHpoTermIdMap, diseases, results, weight);
                            LOGGER.info(String.valueOf(maxoScoreMap));
                            // Take the MaXo term that has the highest score
                            Map.Entry<TermId, Double> maxScore = maxoScoreMap.entrySet().stream().max(Map.Entry.comparingByValue()).get();
                            TermId maxScoreMaxoTermId = maxScore.getKey();
                            double maxScoreValue = maxScore.getValue();
                            String maxScoreTermLabel = diffDiag.getMaxoTermLabel(hpoToMaxoTermMap, maxScoreMaxoTermId);

                            LOGGER.info("Max Score: " + maxScoreMaxoTermId + " (" + maxScoreTermLabel + ")" + " = " + maxScoreValue);
                            double finalScore = diffDiag.finalScore(results, diseaseIds, weight);
                            LOGGER.info("Input Disease List Score: " + finalScore);
                            String backgroundVcf = vcfPath == null ? "" : vcfPath.toFile().getName();
                            TermId diseaseId = phenopacketData.diseaseIds().get(0);
                            writeResults(phenopacketName, backgroundVcf, diseaseId, maxScoreMaxoTermId, maxScoreTermLabel,
                                    n, topNDiseases, diseaseIds, weight, maxScoreValue, printer);
                        }
                    }
                } catch (Exception ex) {
                    continue;
                }
            }
        }

        return 0;
    }

    protected Lirical prepareLirical() throws IOException, LiricalException {
        // Check input.
        List<String> errors = checkInput();
        if (!errors.isEmpty())
            throw new LiricalException(String.format("Errors: %s", String.join(", ", errors)));

        // Bootstrap LIRICAL.
        Lirical lirical = bootstrapLirical();
        return lirical;
    }

    private static BufferedWriter openWriter(Path outputPath) throws IOException {
        return outputPath.toFile().getName().endsWith(".gz")
                ? new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(outputPath))))
                : Files.newBufferedWriter(outputPath);
    }

    @Override
    protected String getGenomeBuild() {
        return genomeBuild;
    }


    protected static PhenopacketData readPhenopacketData(Path phenopacketPath) throws LiricalParseException {
        PhenopacketData data = null;
        try (InputStream is = new BufferedInputStream(Files.newInputStream(phenopacketPath))) {
            PhenopacketImporter v2 = PhenopacketImporters.v2();
            data = v2.read(is);
            LOGGER.debug("Success!");
        } catch (Exception e) {
            LOGGER.debug("Unable to parse as v2 phenopacket, trying v1.");
        }

        if (data == null) {
            try (InputStream is = new BufferedInputStream(Files.newInputStream(phenopacketPath))) {
                PhenopacketImporter v1 = PhenopacketImporters.v1();
                data = v1.read(is);
                LOGGER.debug("Success!");
            } catch (IOException e) {
                LOGGER.debug("Unable to parser as v1 phenopacket.");
                throw new LiricalParseException("Unable to parse phenopacket from " + phenopacketPath.toAbsolutePath());
            }
        }

        // Check we have exactly one disease ID.
        if (data.diseaseIds().isEmpty())
            throw new LiricalParseException("Missing disease ID which is required for the benchmark!");
        else if (data.diseaseIds().size() > 1)
            throw new LiricalParseException("Saw >1 disease IDs {}, but we need exactly one for the benchmark!");
        return data;
    }

    private void writeResultsToFile(Lirical lirical, OutputFormat outputFormat, AnalysisData analysisData,
                                    AnalysisResults results, AnalysisResultsMetadata metadata, String outFilename) throws IOException {
        OutputOptions outputOptions = createOutputOptions(outputDir, outFilename);
        Optional<AnalysisResultsWriter> writer = lirical.analysisResultsWriterFactory().getWriter(outputFormat);
        if (writer.isPresent()) {
            writer.get().process(analysisData, results, metadata, outputOptions);
            outputDir.resolve(outFilename + "." + outputFormatArg.toLowerCase());
        }
        if (compress) {
            zip(outputDir.resolve(outFilename + "." + outputFormatArg.toLowerCase()));
        }
    }

    private static void zip(Path filePath) throws IOException {
        if (Files.isRegularFile(filePath) && Files.isReadable(filePath)) {
            byte[] buffer = new byte[2048];
            FileInputStream inputStream = new FileInputStream(filePath.toString());
            FileOutputStream outputStream = new FileOutputStream(filePath + ".gz");
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                gzipOutputStream.write(buffer, 0, length);
            }
            Files.delete(filePath);
            inputStream.close();
            gzipOutputStream.close();
        }
    }

    /**
     * Write results of a single benchmark into the provided {@code printer}.
     */
    private static void writeResults(String phenopacketName,
                                     String backgroundVcfName,
                                     TermId diseaseId,
                                     TermId maxoId,
                                     String maxoLabel,
                                     double topPosttestProb,
                                     int topNdiseases,
                                     List<TermId> diseaseIds,
                                     double weight,
                                     double score,
                                     CSVPrinter printer) {

        try {
            printer.print(phenopacketName);
            printer.print(backgroundVcfName);
            printer.print(diseaseId);
            printer.print(maxoId);
            printer.print(maxoLabel);
            printer.print(topPosttestProb);
            printer.print(topNdiseases);
            printer.print(diseaseIds);
            printer.print(weight);
            printer.print(score);
            printer.println();
        } catch (IOException e) {
            LOGGER.error("Error writing results for {}: {}", diseaseId, e.getMessage(), e);
        }
    }

}
