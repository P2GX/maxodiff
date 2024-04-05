package org.monarchinitiative.maxodiff.cli.cmd;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.lirical.core.output.AnalysisResultsMetadata;
import org.monarchinitiative.lirical.core.output.AnalysisResultsWriter;
import org.monarchinitiative.lirical.core.output.OutputFormat;
import org.monarchinitiative.lirical.core.output.OutputOptions;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporter;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporters;
import org.monarchinitiative.maxodiff.core.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.DiseaseTermCount;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.MaxoDiffVisualizer;
import org.monarchinitiative.maxodiff.core.analysis.MaxodiffAnalyzer;
import org.monarchinitiative.maxodiff.core.io.InputFileParser;
import org.monarchinitiative.maxodiff.core.io.MaxoDxAnnots;
import org.monarchinitiative.maxodiff.core.io.MaxodiffBuilder;
import org.monarchinitiative.maxodiff.core.io.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.core.service.MaxoDiffService;
import org.monarchinitiative.maxodiff.core.service.PhenotypeService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
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
    protected Path phenopacketPath;

    @CommandLine.Option(names = {"--assembly"},
            paramLabel = "{hg19,hg38}",
            description = "Genome build (default: ${DEFAULT-VALUE}).")
    protected String genomeBuild = "hg38";

    @CommandLine.Option(names = {"--vcf"},
            description = "Path to VCF with background variants.")
    protected Path vcfPath; // nullable

    @CommandLine.Option(names = {"-O", "--outputDirectory"},
//            required = true,
            description = "Where to write the results files.")
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
            description = "Comma-separated list of weight value to use in final score calculation (default: ${DEFAULT-VALUE}).")
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
    protected List<Double> thresholdsArg;


    @Override
    public Integer call() throws Exception {

        Map<String, List<Object>> resultsMap = new HashMap<>();

        resultsMap.put("phenopacketName", new ArrayList<>());
        resultsMap.put("backgroundVcf", new ArrayList<>());
        resultsMap.put("diseaseId", new ArrayList<>());
        resultsMap.put("maxScoreMaxoTermId", new ArrayList<>());
        resultsMap.put("maxScoreTermLabel", new ArrayList<>());
        resultsMap.put("threshold", new ArrayList<>());
        resultsMap.put("topNDiseases", new ArrayList<>());
        resultsMap.put("diseaseIds", new ArrayList<>());
        resultsMap.put("weight", new ArrayList<>());
        resultsMap.put("maxScoreValue", new ArrayList<>());

        MaxodiffDataResolver dataResolver = new MaxodiffDataResolver(dataSection.liricalDataDirectory);
        MaxoDxAnnots maxoDxAnnots = new MaxoDxAnnots(dataResolver.maxoDxAnnots());
        Map<SimpleTerm, Set<SimpleTerm>> fullHpoToMaxoTermMap = maxoDxAnnots.getSimpleTermSetMap();

        List<Double> weights = new ArrayList<>();
        weightsArg.stream().forEach(w -> weights.add(w));
        List<Double> filterPosttestProbs = new ArrayList<>();
        thresholdsArg.stream().forEach(t -> filterPosttestProbs.add(t));

        try {
            // Read phenopacket data.
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

            for (double posttestFilter : filterPosttestProbs) {
                LOGGER.info("Min Posttest Probabiltiy Threshold = " + posttestFilter);
                // Collect HPO terms and frequencies for the target m diseases
                List<TermId> diseaseIds = new ArrayList<>();
                List<TestResult> testResults = results.resultsWithDescendingPostTestProbability()
                        .filter(r -> r.posttestProbability() >= posttestFilter)
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
//                    double finalScore = diffDiag.finalScore(results, diseaseIds, weight);
//                    LOGGER.info("Input Disease List Score: " + finalScore);
                    String backgroundVcf = vcfPath == null ? "" : vcfPath.toFile().getName();
                    TermId diseaseId = phenopacketData.diseaseIds().get(0);

                    List<Object> phenopacketNames = resultsMap.get("phenopacketName");
                    phenopacketNames.add(phenopacketName);
                    List<Object> backgroundVcfs = resultsMap.get("backgroundVcf");
                    backgroundVcfs.add(backgroundVcf);
                    List<Object> diseaseIdList = resultsMap.get("diseaseId");
                    diseaseIdList.add(diseaseId);
                    List<Object> maxScoreMaxoTermIds = resultsMap.get("maxScoreMaxoTermId");
                    maxScoreMaxoTermIds.add(maxScoreMaxoTermId);
                    List<Object> maxScoreTermLabels = resultsMap.get("maxScoreTermLabel");
                    maxScoreTermLabels.add(maxScoreTermLabel);
                    List<Object> posttestFilters = resultsMap.get("threshold");
                    posttestFilters.add(posttestFilter);
                    List<Object> topNDiseasesList = resultsMap.get("topNDiseases");
                    topNDiseasesList.add(topNDiseases);
                    List<Object> diseaseIdsList = resultsMap.get("diseaseIds");
                    diseaseIdsList.add(diseaseIds);
                    List<Object> weightList = resultsMap.get("weight");
                    weightList.add(weight);
                    List<Object> maxScoreValues = resultsMap.get("maxScoreValue");
                    maxScoreValues.add(maxScoreValue);
                    resultsMap.replace("phenopacketName", phenopacketNames);
                    resultsMap.replace("backgroundVcf", backgroundVcfs);
                    resultsMap.replace("diseaseId", diseaseIdList);
                    resultsMap.replace("maxScoreMaxoTermId", maxScoreMaxoTermIds);
                    resultsMap.replace("maxScoreTermLabel", maxScoreTermLabels);
                    resultsMap.replace("threshold", posttestFilters);
                    resultsMap.replace("topNDiseases", topNDiseasesList);
                    resultsMap.replace("diseaseIds", diseaseIdsList);
                    resultsMap.replace("weight", weightList);
                    resultsMap.replace("maxScoreValue", maxScoreValues);
                }
            }
            BatchDiagnosisCommand.setResultsMap(resultsMap);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
            resultsMap = new HashMap<>();
            BatchDiagnosisCommand.setResultsMap(resultsMap);
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



}
