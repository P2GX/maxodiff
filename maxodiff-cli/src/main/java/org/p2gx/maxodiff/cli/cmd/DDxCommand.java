package org.p2gx.maxodiff.cli.cmd;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.*;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.io.impl.MdContextBuilder;
import org.p2gx.maxodiff.core.MaxoDiffAnalysisResultRow;
import org.p2gx.maxodiff.core.MaxodiffAnalysisRunner;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.p2gx.maxodiff.core.io.JsonWriter;
import org.p2gx.maxodiff.core.model.PhenopacketData;
import org.p2gx.maxodiff.html.results.tleaf.TleafResults;
import org.p2gx.maxodiff.core.phenomizer.PhenomizerDDxEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPOutputStream;


/**
 * This command performs the maxodiff algorithm for a single phenopacket.
 */
@CommandLine.Command(
        name = "analyze",
        aliases = {"A"},
        mixinStandardHelpOptions = true,
        description = "Analyze one Phenopacket")
public class DDxCommand extends BaseCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DDxCommand.class);

    @CommandLine.Option(
            names = {"-m", "--maxoData"},
            description = "Path to maxo data directory (default: ${DEFAULT-VALUE}).")
    protected Path maxoDataPath = Path.of("data");

    @CommandLine.Option(
            names = {"-p", "--phenopacket"},
            required = true,
            description = "Path to phenopacket JSON file.")
    protected Path phenopacketPath;

    @CommandLine.Option(names = {"-O", "--outputDirectory"},
            description = "Where to write the results files (default: ${DEFAULT-VALUE}).")
    protected Path outputDir = Path.of(".");

    @CommandLine.Option(names = {"-f", "--outFilename"},
            description = "Specific results output file name (default: ${DEFAULT-VALUE}).")
    protected String outputFilenameArg = "";

    @CommandLine.Option(names = {"-j", "--json"},
        description = "output results to JSON file")
    private boolean outputJson = false;


    @CommandLine.Option(names = {"--csv"},
            description = "Output results as CSV.")
    private boolean writeCsv = false;

    @CommandLine.Option(names = {"--diseaseProbModel"},
            paramLabel = "{ranked}",
            description = "Disease Probability Model to use for Rank MAxO algorithm (\"ranked\", \"softmax\", \"expDecay\". Default: ${DEFAULT-VALUE}).")
    protected String diseaseProbModel = "ranked";

    @CommandLine.Option(names = {"-nr", "--nRepetitions"},
            description = "Number of repetitions for running differential diagnosis.")
    protected Integer nRepetitions = 100;

    @CommandLine.Option(names = {"-sd", "--singleDisease"},
            description = "Single Disease Id for analysis.")
    protected String singleDisease = null;

    @Override
    public Integer execute() throws Exception {
        if (!Files.exists(phenopacketPath)) {
            System.err.println("Could not find phenopacket file: " + phenopacketPath);
            return 1;
        }
        try {
            return runSingleMaxodiffAnalysis(phenopacketPath);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            return 1;
        }
    }

    protected int runSingleMaxodiffAnalysis(Path phenopacketPath) throws Exception {
        MdContext context = MdContextBuilder.buildContext(
                this.maxoDataPath,
                this.nRepetitions,
                this.nDiseases,
                true);
        LOGGER.info("{}", context);
        try {
            List<HpoFrequency> allHpoFrequencies = context.createHpoFrequencies();
            DiffDiagRefiner maxoDiffRefiner = context.createRefiner();
            DDxEngine engine = new PhenomizerDDxEngine(context);
            PhenopacketData phenopacketData = PhenopacketData.readPhenopacketData(phenopacketPath);
            MaxodiffAnalysisRunner runner = new MaxodiffAnalysisRunner(
                    context,
                    engine,
                    maxoDiffRefiner,
                    allHpoFrequencies);
            if (writeCsv) {
                MaxoDiffAnalysisResultRow row = runner.batchAnalysis(phenopacketData);
                writeCsvResults(phenopacketData.sampleId(), row);
            } else if (singleDisease != null) {
                TermId targetDiseaseId = TermId.of(singleDisease);
                List<RankedMaxoResultSingleDisease> resultsList = runner.analyzeSampleSingleDisease(phenopacketData,
                                                                                                    targetDiseaseId);
                if (resultsList.isEmpty()) {
                    // should never happen...
                    System.err.println("No results found for phenopacket: " + phenopacketPath);
                    return 1;
                }

                RankedMaxoResultSingleDisease topResult = resultsList.getFirst();
                String maxIcMaxoTerm = topResult.maxoTerm().toString();
                double maxIcValue = topResult.totalIC();
                System.out.println("Target Disease: " + topResult.targetDisease());
                System.out.println("Max IC: " + maxIcMaxoTerm + " = " + maxIcValue);
                System.out.println(resultsList.subList(0,10));

            } else {
                List<RankedMaxoResult> resultsList = runner.analyzeSample(phenopacketData);
                if (resultsList.isEmpty()) {
                    // should never happen...
                    System.err.println("No results found for phenopacket: " + phenopacketPath);
                    return 1;
                }
                // Take the MaXo term that has the highest score
                RankedMaxoResult topResult = resultsList.getFirst();
                String maxScoreMaxoTerm = topResult.maxoTerm().toString();
                double maxScoreValue = topResult.maxoScore();
                System.out.println("Max Score: " + maxScoreMaxoTerm + " = " + maxScoreValue);

                String jsonFilename = String.join("_", phenopacketData.sampleId(),
                        nDiseases.toString(), nRepetitions.toString(), "maxodiff_results.json");
                if (!outputFilenameArg.isEmpty()) {
                    jsonFilename = outputFilenameArg + ".json";
                }
                if (outputJson) {
                    Path jsonPath = Path.of(String.join(File.separator, outputDir.toString(), jsonFilename));
                    JsonWriter.writeToJsonFile(jsonPath, resultsList);
                    LOGGER.info("Wrote JSON file to {}.", jsonPath);
                    return 0;
                }

                MdMetadata mdMetadata = new MdMetadata(phenopacketData.sampleId(),
                        this.nDiseases,
                        this.nRepetitions,
                        phenopacketData.observed(),
                        phenopacketData.excluded(),
                        resultsList);

                HTMLFrequencyMap htmlFrequencyMap = new HTMLFrequencyMap(context);
                String headerHtml = TleafResults.writeHTMLResults(mdMetadata, resultsList, htmlFrequencyMap);
                String fileName = "mdResults.html";
                if (!outputFilenameArg.isEmpty()) {
                    fileName = outputFilenameArg + ".html";
                }
                Path maxodiffResultsHTMLPath = Path.of(String.join(File.separator, outputDir.toString(), fileName));
                Files.writeString(maxodiffResultsHTMLPath, headerHtml);
                LOGGER.info("Wrote HTML file to {}", maxodiffResultsHTMLPath);
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return 1;
        }
        return 0;
    }


    private int writeCsvResults(String phenopacketName, MaxoDiffAnalysisResultRow row)  {
        String outputFilename = String.join("_", phenopacketName, "maxodiff", "results.csv");
        if (!outputFilenameArg.isEmpty()) {
            outputFilename = outputFilenameArg + ".csv";
        }
        if (!outputDir.toFile().isDirectory()) {
            System.err.println("Output directory does not exist: '" + outputDir +
                    "'. Create the directory and rerun this command.");
            return 1;
        }
        Path maxodiffResultsFilePath = Path.of(String.join(File.separator, outputDir.toString(), outputFilename));
        try (BufferedWriter writer = openOutputFileWriter(maxodiffResultsFilePath);
             CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord(MaxoDiffAnalysisResultRow.headerFields());
            printer.printRecord(row.getFields());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            return 1;
        }

        System.out.println("Wrote output to " + outputFilename);
        return 0;
    }

    protected static BufferedWriter openOutputFileWriter(Path outputPath) throws IOException {
        return outputPath.toFile().getName().endsWith(".gz")
                ? new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(outputPath))))
                : Files.newBufferedWriter(outputPath);
    }


}
