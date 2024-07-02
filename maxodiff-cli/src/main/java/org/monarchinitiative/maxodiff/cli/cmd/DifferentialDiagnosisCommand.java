package org.monarchinitiative.maxodiff.cli.cmd;

import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.lirical.core.output.AnalysisResultsMetadata;
import org.monarchinitiative.lirical.core.output.AnalysisResultsWriter;
import org.monarchinitiative.lirical.core.output.OutputFormat;
import org.monarchinitiative.lirical.core.output.OutputOptions;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporter;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporters;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.config.MaxodiffPropsConfiguration;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.io.PhenopacketFileParser;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.lirical.LiricalDifferentialDiagnosisEngineConfigurer;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPOutputStream;


/**
 * Perform Differential Diagnosis calculations
 */

@CommandLine.Command(name = "diagnosis", aliases = {"d"},
        mixinStandardHelpOptions = true,
        description = "maxodiff analysis")
public class DifferentialDiagnosisCommand extends BaseLiricalCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DifferentialDiagnosisCommand.class);

    @CommandLine.Option(names = {"-m", "--maxoData"},
            required = true,
            description = "Path to MaXo data directory.")
    protected Path maxoDataPath;

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

    @CommandLine.Option(names = {"-n", "--nDiseases"},
//            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of n diseases for filtering diseases to include in differential diagnosis.")
    protected List<Integer> nDiseasesArg;


    @Override
    public Integer call() throws Exception {

        Map<String, List<Object>> resultsMap = new HashMap<>();

        resultsMap.put("phenopacketName", new ArrayList<>());
        resultsMap.put("backgroundVcf", new ArrayList<>());
        resultsMap.put("diseaseId", new ArrayList<>());
        resultsMap.put("maxScoreMaxoTermId", new ArrayList<>());
        resultsMap.put("maxScoreTermLabel", new ArrayList<>());
        resultsMap.put("topNDiseases", new ArrayList<>());
        resultsMap.put("diseaseIds", new ArrayList<>());
        resultsMap.put("weight", new ArrayList<>());
        resultsMap.put("maxScoreValue", new ArrayList<>());


        List<Double> weights = new ArrayList<>();
        weightsArg.stream().forEach(w -> weights.add(w));
        List<Integer> nDiseasesList = new ArrayList<>();
        nDiseasesArg.stream().forEach(n -> nDiseasesList.add(n));

        System.out.println(weights);
        System.out.println(nDiseasesList);

        Lirical lirical = bootstrapLirical();
        try (LiricalAnalysisRunner runner = lirical.analysisRunner()) {
            LiricalDifferentialDiagnosisEngineConfigurer configurer = LiricalDifferentialDiagnosisEngineConfigurer.of(runner);
            var analysisOptions = prepareAnalysisOptions(lirical);
            DifferentialDiagnosisEngine engine = configurer.configure(analysisOptions);
            
            PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(phenopacketPath);
            Sample sample = Sample.of(phenopacketData.sampleId(),
                    phenopacketData.presentHpoTermIds().toList(),
                    phenopacketData.excludedHpoTermIds().toList());

            // Get initial differential diagnoses from running LIRICAL
            List<DifferentialDiagnosis> differentialDiagnoses = engine.run(sample);

            // Summarize the LIRICAL results.
            //String sampleId = analysisData.sampleId();
            String phenopacketName = phenopacketPath.toFile().getName();
            //String outFilename = String.join("_",
            //        phenopacketName.replace(".json", ""),
            //        "lirical",
            //        "results");
            //AnalysisResultsMetadata metadata = prepareAnalysisResultsMetadata(gene2Genotypes, lirical, sampleId);
            //writeResultsToFile(lirical, OutputFormat.parse(outputFormatArg), analysisData, results, metadata, outFilename);

            // Make maxodiffRefiner
            MaxodiffDataResolver maxodiffDataResolver = MaxodiffDataResolver.of(maxoDataPath);
            MaxodiffPropsConfiguration maxodiffPropsConfiguration = MaxodiffPropsConfiguration.createConfig(maxodiffDataResolver);

            DiffDiagRefiner maxoDiffRefiner = maxodiffPropsConfiguration.diffDiagRefiner(false);
            BiometadataService biometadataService = maxodiffPropsConfiguration.biometadataService();

            //TODO? get list of diseases from LIRICAL results, and add diseases from CLI arg to total list for analysis

            System.out.println(weights);
            System.out.println(nDiseasesList);
            for (int nDiseases : nDiseasesList) {
                System.out.println("n Diseases = " + nDiseases);
                // Make MaXo:HPO Term Map
//                Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoTermMap = maxoTermMap.makeMaxoToHpoTermMap(results, null,
//                        phenopacketPath, nDiseases);
//
//                LOGGER.info(String.valueOf(maxoToHpoTermMap));

                for (double weight : weights) {
                    System.out.println("Weight = " + weight);
                    // Get List of Refinement results: maxo term scores and frequencies
                    RefinementOptions options = RefinementOptions.of(nDiseases, weight);
                    RefinementResults refinementResults = maxoDiffRefiner.run(sample, differentialDiagnoses, options);

                    List<MaxodiffResult> resultsList = refinementResults.maxodiffResults().stream().toList();
                    TermId diseaseId = phenopacketData.diseaseIds().get(0);
                    // Take the MaXo term that has the highest score
                    MaxodiffResult topResult = resultsList.get(0);
                    String maxScoreMaxoTermId = topResult.maxoTermScore().maxoId();
                    String maxScoreTermLabel = biometadataService.maxoLabel(maxScoreMaxoTermId).orElse("unknown");
                    double maxScoreValue = topResult.maxoTermScore().score();

                    System.out.println("Max Score: " + maxScoreMaxoTermId + " (" + maxScoreTermLabel + ")" + " = " + maxScoreValue);

                    String backgroundVcf = vcfPath == null ? "" : vcfPath.toFile().getName();
                    Set<TermId> diseaseIds = topResult.maxoTermScore().omimTermIds();
                    int topNDiseases = diseaseIds.size();

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
                    resultsMap.replace("topNDiseases", topNDiseasesList);
                    resultsMap.replace("diseaseIds", diseaseIdsList);
                    resultsMap.replace("weight", weightList);
                    resultsMap.replace("maxScoreValue", maxScoreValues);
                }
            }
            BatchDiagnosisCommand.setResultsMap(resultsMap);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
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

    // private void writeResultsToFile(Lirical lirical, OutputFormat outputFormat, AnalysisData analysisData,
    //                                 AnalysisResults results, AnalysisResultsMetadata metadata, String outFilename) throws IOException {
    //     OutputOptions outputOptions = createOutputOptions(outputDir, outFilename);
    //     Optional<AnalysisResultsWriter> writer = lirical.analysisResultsWriterFactory().getWriter(outputFormat);
    //     if (writer.isPresent()) {
    //         writer.get().process(analysisData, results, metadata, outputOptions);
    //         outputDir.resolve(outFilename + "." + outputFormatArg.toLowerCase());
    //     }
    //     if (compress) {
    //         zip(outputDir.resolve(outFilename + "." + outputFormatArg.toLowerCase()));
    //     }
    // }

    // private static void zip(Path filePath) throws IOException {
    //     if (Files.isRegularFile(filePath) && Files.isReadable(filePath)) {
    //         byte[] buffer = new byte[2048];
    //         FileInputStream inputStream = new FileInputStream(filePath.toString());
    //         FileOutputStream outputStream = new FileOutputStream(filePath + ".gz");
    //         GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
    //         int length;
    //         while ((length = inputStream.read(buffer)) > 0) {
    //             gzipOutputStream.write(buffer, 0, length);
    //         }
    //         Files.delete(filePath);
    //         inputStream.close();
    //         gzipOutputStream.close();
    //     }
    // }



}
