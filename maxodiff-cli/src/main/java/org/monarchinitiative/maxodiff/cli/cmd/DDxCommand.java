package org.monarchinitiative.maxodiff.cli.cmd;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.maxodiff.config.MaxoDiffLoader;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.config.MaxodiffPropsConfiguration;
import org.monarchinitiative.maxodiff.core.MaxoDiffAnalysisResultRow;
import org.monarchinitiative.maxodiff.core.MaxodiffAnalysisRunner1;
import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.RankedMaxoResult;
import org.monarchinitiative.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.io.JsonWriter;
import org.monarchinitiative.maxodiff.core.model.PhenopacketData;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.html.results.HtmlResults;
import org.monarchinitiative.maxodiff.phenomizer.IcMicaData;
import org.monarchinitiative.maxodiff.phenomizer.PhenomizerDifferentialDiagnosisEngine;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
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
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;


/**
 * This command performs the maxodiff algorithm for a single phenopacket.
 */
@CommandLine.Command(
        name = "analyze1",
        aliases = {"a1"},
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
            Sample sample = phenopacketData.getSample();

            MaxodiffAnalysisRunner1 runner = new MaxodiffAnalysisRunner1(this.nDiseases,
                    this.nRepetitions,
                    engine,
                    maxoDiffRefiner,
                    biometadataService);
            if (writeCsv) {
                MaxoDiffAnalysisResultRow row = runner.batchAnalysis(phenopacketData);
                writeCsvResults(sample.id(), row);
            } else {
                List<RankedMaxoResult> resultsList = runner.analyzeSample(phenopacketData);
                // Take the MaXo term that has the highest score
                RankedMaxoResult topResult = resultsList.getFirst();
                String maxScoreMaxoTerm = topResult.maxoTerm().toString();
                double maxScoreValue = topResult.maxoScore();
                System.out.println("Max Score: " + maxScoreMaxoTerm + " = " + maxScoreValue);

                String jsonFilename = String.join("_", sample.id(),
                        nDiseases.toString(), nRepetitions.toString(), "maxodiff_results", ".json");
                Path jsonPath = Path.of(String.join(File.separator, outputDir.toString(), jsonFilename));
                JsonWriter.writeToJsonFile(jsonPath, resultsList);

//                writeHtmlResults(resultsList, biometadataService,
//                    outputDir, sample, hpoDiseases, maxoResult.hpoTermCounts(),
//                    icMicaDict);

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

    private void writeHtmlResults(List<MaxodiffResult> resultsList,
                                  BiometadataService biometadataService,
                                  Path outputDir,
                                  Sample sample,
                                  HpoDiseases hpoDiseases,
                                  Map<TermId, List<HpoFrequency>> hpoTermCounts,
                                  Map<TermPair, Double> icMicaDict) throws Exception {
        String nDiseasesAbbr = String.join("", "n", String.valueOf(this.nDiseases));
        String nRepsAbbr = String.join("", "nr", String.valueOf(this.nRepetitions));
        String outputFilename = String.join("_", sample.id(),
            nDiseasesAbbr, nRepsAbbr, "maxodiff", "results1.html");
        Path maxodiffResultsHTMLPath = Path.of(String.join(File.separator, outputDir.toString(), outputFilename));

        String htmlString = HtmlResults.writeHTMLResults(
                sample,
                this.nDiseases,
                hpoDiseases,
                this.nRepetitions,
                resultsList,
                biometadataService,
                hpoTermCounts,
                icMicaDict);

        Files.writeString(maxodiffResultsHTMLPath, htmlString);
        LOGGER.info("Wrote HTML file to {}", maxodiffResultsHTMLPath);
    }

    protected static BufferedWriter openOutputFileWriter(Path outputPath) throws IOException {
        return outputPath.toFile().getName().endsWith(".gz")
                ? new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(outputPath))))
                : Files.newBufferedWriter(outputPath);
    }


}
