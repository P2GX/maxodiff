package org.p2gx.maxodiff.cli.cmd;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.p2gx.maxodiff.core.analysis.*;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.io.impl.MdContextBuilder;
import org.p2gx.maxodiff.cli.util.IoUtil;
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
@CommandLine.Command(name = "analyze", aliases = {
        "A" }, mixinStandardHelpOptions = true, description = "Analyze one Phenopacket")
public class DDxCommand extends BaseCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DDxCommand.class);

    @CommandLine.Option(names = { "-m",
            "--maxoData" }, description = "Path to maxo data directory (default: ${DEFAULT-VALUE}).")
    protected Path maxoDataPath = Path.of("data");

    @CommandLine.Option(names = { "-p",
            "--phenopacket" }, required = true, description = "Path to phenopacket JSON file.")
    protected Path phenopacketPath;

    @CommandLine.Option(names = { "-O",
            "--outputDirectory" }, description = "Where to write the results files (default: ${DEFAULT-VALUE}).")
    protected Path outputDir = Path.of(".");

    @CommandLine.Option(names = { "-f",
            "--outFilename" }, description = "Specific results output file name (default: ${DEFAULT-VALUE}).")
    protected String outputFilenameArg = "";

    @CommandLine.Option(names = { "-j", "--json" }, description = "output results to JSON file")
    private boolean outputJson = false;

    @CommandLine.Option(names = {
            "--diseaseProbModel" }, paramLabel = "{ranked}", description = "Disease Probability Model to use for Rank MAxO algorithm (\"ranked\", \"softmax\", \"expDecay\". Default: ${DEFAULT-VALUE}).")
    protected String diseaseProbModel = "ranked";

    @CommandLine.Option(names = { "-nr",
            "--nRepetitions" }, description = "Number of repetitions for running differential diagnosis.")
    protected Integer nRepetitions = 100;

    @CommandLine.Option(names = { "-sd", "--singleDisease" }, description = "Single Disease Id for analysis.")
    protected String singleDisease = null;

    @CommandLine.Option(names = { "-t", "--nThreads" }, description = "Number of threads to use for analysis.")
    protected int nThreads = Runtime.getRuntime().availableProcessors() - 1;

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
                    nThreads,
                    engine,
                    maxoDiffRefiner,
                    allHpoFrequencies);
            List<RankedMaxoResult> resultsList = runner.analyzeSample(phenopacketData);
            if (resultsList.isEmpty()) {
                // should never happen...
                System.err.println("No results found for phenopacket: " + phenopacketPath);
                return 1;
            }
            LOGGER.debug("Analysis complete.");
            // Take the MaXo term that has the highest score
            RankedMaxoResult topResult = resultsList.getFirst();
            String maxScoreMaxoTerm = topResult.maxoTerm().toString();
            double maxScoreValue = topResult.maxoScore();
            System.out.println("Max Score: " + maxScoreMaxoTerm + " = " + maxScoreValue);

            if (outputJson) {
                Path jsonPath = IoUtil.defaultPath(phenopacketData.sampleId(), nDiseases, nRepetitions, "json");
                if (!outputFilenameArg.isEmpty()) {
                    jsonPath = IoUtil.resolveOutputFile(outputFilenameArg, outputDir, "json");
                }
                LOGGER.debug("Creating JSON file: {}.", jsonPath);
                int zeroIdx = resultsList.stream()
                        .filter(result -> result.maxoScore() == 0.)
                        .findFirst().map(resultsList::indexOf).orElse(resultsList.size());
                int nDisplayed = Math.min(resultsList.size(), zeroIdx);
                JsonWriter.writeToJsonFile(jsonPath, resultsList.subList(0, nDisplayed));
                LOGGER.debug("Wrote JSON file to {}.", jsonPath);
                return 0;
            }

            LOGGER.debug("Writing HTML file.");
            MdMetadata mdMetadata = new MdMetadata(phenopacketData.sampleId(),
                    this.nDiseases,
                    this.nRepetitions,
                    phenopacketData.observed(),
                    phenopacketData.excluded(),
                    resultsList);

            HTMLFrequencyMap htmlFrequencyMap = new HTMLFrequencyMap(context);
            String headerHtml = TleafResults.writeHTMLResults(mdMetadata, resultsList, htmlFrequencyMap);
            Path htmlPath = IoUtil.defaultPath(phenopacketData.sampleId(), nDiseases, nRepetitions, "html");
            if (!outputFilenameArg.isEmpty()) {
                htmlPath = IoUtil.resolveOutputFile(outputFilenameArg, outputDir, "json");
            }
            Files.writeString(htmlPath, headerHtml);
            LOGGER.info("Wrote HTML file to {}", htmlPath);

        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return 1;
        }
        return 0;
    }

    protected static BufferedWriter openOutputFileWriter(Path outputPath) throws IOException {
        return outputPath.toFile().getName().endsWith(".gz")
                ? new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(outputPath))))
                : Files.newBufferedWriter(outputPath);
    }

}
