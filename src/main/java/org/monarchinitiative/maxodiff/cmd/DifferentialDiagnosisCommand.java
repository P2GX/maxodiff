package org.monarchinitiative.maxodiff.cmd;

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
import org.monarchinitiative.maxodiff.analysis.MaxoDiffVisualizer;
import org.monarchinitiative.maxodiff.analysis.MaxodiffAnalyzer;
import org.monarchinitiative.maxodiff.io.InputFileParser;
import org.monarchinitiative.maxodiff.io.MaxodiffBuilder;
import org.monarchinitiative.maxodiff.service.MaxoDiffService;
import org.monarchinitiative.maxodiff.service.PhenotypeService;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
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
            required = true,
            arity = "1..*",
            description = "Path(s) to phenopacket JSON file(s).")
    protected List<Path> phenopacketPaths;

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
            description = "Weight value to use in final score calculation (default: ${DEFAULT-VALUE}).")
    public double weight = 0.5;

    @CommandLine.Option(names = {"-l", "--diseaseList"},
            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of diseases to include in differential diagnosis.")
    protected List<String> diseaseIdsArg;


    @Override
    public Integer call() throws Exception {

        Lirical lirical = prepareLirical();

        for (int i = 0; i < phenopacketPaths.size(); i++) {
            // Read phenopacket data.
            Path phenopacketPath = phenopacketPaths.get(i);
            PhenopacketData phenopacketData = readPhenopacketData(phenopacketPath);

            // Prepare LIRICAL analysis options
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
            writeResultsToFile(lirical, OutputFormat.parse(outputFormatArg), analysisData, results, metadata, outFilename);

            // Calculate Differential Diagnosis Score
            DifferentialDiagnosis diffDiag = new DifferentialDiagnosis();
            List<TermId> diseaseIds = new ArrayList<>();
            diseaseIdsArg.stream().forEach(id -> diseaseIds.add(TermId.of(id)));
            System.out.println(diseaseIds);
            System.out.println("posttest probability sum = " + diffDiag.posttestProbabilitySum(results, diseaseIds));
            double finalScore = diffDiag.finalScore(results, diseaseIds, weight);
            System.out.println("final Score = " + finalScore);

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
