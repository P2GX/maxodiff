package org.monarchinitiative.maxodiff.cli.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.config.MaxodiffPropsConfiguration;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.io.PhenopacketFileParser;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.lirical.LiricalDifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.lirical.LiricalDifferentialDiagnosisEngineConfigurer;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;


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

        Lirical lirical = bootstrapLirical();
        try (LiricalAnalysisRunner runner = lirical.analysisRunner()) {
            LiricalDifferentialDiagnosisEngineConfigurer configurer = LiricalDifferentialDiagnosisEngineConfigurer.of(runner);
            AnalysisOptions analysisOptions = prepareAnalysisOptions(lirical);
            LiricalDifferentialDiagnosisEngine engine = configurer.configure(analysisOptions);

            for (int i = 0; i < phenopacketPaths.size(); i++) {
                try {

                    Path pPath = phenopacketPaths.get(i);
                    PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(pPath);
                    Sample sample = Sample.of(phenopacketData.sampleId(),
                            phenopacketData.presentHpoTermIds().toList(),
                            phenopacketData.excludedHpoTermIds().toList());


                    System.out.println(weights);
                    System.out.println(nDiseasesList);

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

                    // Make maxodiffRefiner
                    MaxodiffDataResolver maxodiffDataResolver = MaxodiffDataResolver.of(maxoDataPath);
                    MaxodiffPropsConfiguration maxodiffPropsConfiguration = MaxodiffPropsConfiguration.createConfig(maxodiffDataResolver);

                    Map<String, DiffDiagRefiner> refiners = new HashMap<>();
                    refiners.put("MaxoDiff", maxodiffPropsConfiguration.diffDiagRefiner(false));
                    if (dummy) {
                        refiners.put("Dummy", maxodiffPropsConfiguration.diffDiagRefiner(true));
                    }

                    BiometadataService biometadataService = maxodiffPropsConfiguration.biometadataService();

                    //TODO? get list of diseases from LIRICAL results, and add diseases from CLI arg to total list for analysis
                    for (int nDiseases : nDiseasesList) {
                        System.out.println("n Diseases = " + nDiseases);
                        // Make MaXo:HPO Term Map
    //                Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoTermMap = maxoTermMap.makeMaxoToHpoTermMap(results, null,
    //                        phenopacketPath, nDiseases);
    //
    //                LOGGER.info(String.valueOf(maxoToHpoTermMap));

                        for (double weight : weights) {
                            RefinementOptions options = RefinementOptions.of(nDiseases, weight);
                            for (Map.Entry<String, DiffDiagRefiner> e : refiners.entrySet()) {
                                RefinementResults refinementResults = e.getValue().run(sample, differentialDiagnoses, options);
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
                                double maxScoreValue = topResult.maxoTermScore().score();

                                System.out.println("Max Score: " + maxScoreMaxoTermId + " (" + maxScoreTermLabel + ")" + " = " + maxScoreValue);

                            }
                            System.out.println("Weight = " + weight);

                            TermId diseaseId = phenopacketData.diseaseIds().get(0);
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.info(ex.getMessage());
                }
            }
        }

        return 0;
    }


    public void writeToJsonFile(Path filePath, RefinementResults results) throws IOException {
        ObjectWriter writer = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();
        writer.writeValue(new File(filePath.toString()), results);
    }


}
