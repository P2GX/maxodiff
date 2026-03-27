package org.p2gx.maxodiff.cli.cmd;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.p2gx.maxodiff.config.MaxoDiffLoader;
import org.p2gx.maxodiff.config.MaxodiffDataResolver;
import org.p2gx.maxodiff.config.MaxodiffPropsConfiguration;
import org.p2gx.maxodiff.core.MaxoDiffAnalysisResultRow;
import org.p2gx.maxodiff.core.MaxodiffAnalysisRunner;
import org.p2gx.maxodiff.core.analysis.HTMLFrequencyMap;
import org.p2gx.maxodiff.core.analysis.MdMetadata;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.p2gx.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.p2gx.maxodiff.core.io.JsonWriter;
import org.p2gx.maxodiff.core.model.PhenopacketData;
import org.p2gx.maxodiff.core.model.PpktSample;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.p2gx.maxodiff.html.results.tleaf.TleafResults;
import org.p2gx.maxodiff.phenomizer.IcMicaData;
import org.p2gx.maxodiff.phenomizer.PhenomizerDifferentialDiagnosisEngine;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        String phenopacketName = phenopacketPath.toFile().getName();
        // Load ontology and hpo diseases
        MaxoDiffLoader mdloader =  MaxoDiffLoader.fileLoader(maxoDataPath);
        HpoDiseases hpoDiseases = mdloader.hpoDiseases();
        IcMicaData icMicaData = mdloader.icMicaData();

        try {
            // Make maxodiffRefiner
            MaxodiffDataResolver maxodiffDataResolver = MaxodiffDataResolver.of(maxoDataPath);
            MaxodiffPropsConfiguration maxodiffPropsConfiguration = MaxodiffPropsConfiguration.createConfig(maxodiffDataResolver);

            DiffDiagRefiner maxoDiffRefiner = maxodiffPropsConfiguration.diffDiagRefiner();
            BiometadataService biometadataService = maxodiffPropsConfiguration.biometadataService();

            // Configure Phenomizer engine
            Map<TermPair, Double> icMicaDict = icMicaData.icMicaDict();
            DifferentialDiagnosisEngine engine = new PhenomizerDifferentialDiagnosisEngine(hpoDiseases, icMicaDict);

            // Read phenopacket data and make sample
            PhenopacketData phenopacketData = PhenopacketData.readPhenopacketData(phenopacketPath);
            List<SimpleTerm> observedSampleTerms = new ArrayList<>();
            List<SimpleTerm> excludedSampleTerms = new ArrayList<>();
            phenopacketData.observedHpoTermIds().forEach(tid ->
                    observedSampleTerms.add(new SimpleTerm(tid.getValue(), biometadataService.hpoLabel(tid).orElse("n/a"))));
            phenopacketData.excludedHpoTermIds().forEach(tid ->
                    excludedSampleTerms.add(new SimpleTerm(tid.getValue(), biometadataService.hpoLabel(tid).orElse("n/a"))));
            PpktSample ppktSample = new PpktSample(phenopacketData.sampleId(), observedSampleTerms, excludedSampleTerms);

            MaxodiffAnalysisRunner runner = new MaxodiffAnalysisRunner(this.nDiseases,
                    this.nRepetitions,
                    engine,
                    maxoDiffRefiner,
                    biometadataService);
            if (writeCsv) {
                MaxoDiffAnalysisResultRow row = runner.batchAnalysis(phenopacketData);
                writeCsvResults(ppktSample.id(), row);
            } else {
                List<RankedMaxoResult> resultsList = runner.analyzeSample(phenopacketData);
                // Take the MaXo term that has the highest score
                RankedMaxoResult topResult = resultsList.getFirst();
                String maxScoreMaxoTerm = topResult.maxoTerm().toString();
                double maxScoreValue = topResult.maxoScore();
                System.out.println("Max Score: " + maxScoreMaxoTerm + " = " + maxScoreValue);

                String jsonFilename = String.join("_", ppktSample.id(),
                        nDiseases.toString(), nRepetitions.toString(), "maxodiff_results", ".json");
                if (outputJson) {
                    Path jsonPath = Path.of(String.join(File.separator, outputDir.toString(), jsonFilename));
                    JsonWriter.writeToJsonFile(jsonPath, resultsList);
                    LOGGER.info("Wrote JSON file to {}.", jsonPath);
                    return 0;
                }

                MdMetadata mdMetadata = new MdMetadata(ppktSample.id(),
                        this.nDiseases,
                        this.nRepetitions,
                        ppktSample.observedHpoTerms(),
                        ppktSample.excludedHpoTerms(),
                        resultsList);

                HTMLFrequencyMap htmlFrequencyMap = new HTMLFrequencyMap(hpoDiseases, icMicaData.icMicaDict());

                String headerHtml = TleafResults.writeHTMLResults(mdMetadata, resultsList, htmlFrequencyMap);

                Path maxodiffResultsHTMLPath = Path.of(String.join(File.separator, outputDir.toString(), "mdResults.html"));

                Files.writeString(maxodiffResultsHTMLPath, headerHtml);
                LOGGER.info("Wrote HTML file to {}", maxodiffResultsHTMLPath);

            }


           // BatchDiagnosisCommand.setResultsMap(resultsMap);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
            return 1;
        }
        return 0;
    }


    private int writeCsvResults(String phenopacketName, MaxoDiffAnalysisResultRow row)  {
        String outputFilename = String.join("_", phenopacketName, "maxodiff", "results.csv");
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
