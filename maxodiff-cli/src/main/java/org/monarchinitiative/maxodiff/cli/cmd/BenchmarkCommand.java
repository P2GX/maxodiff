package org.monarchinitiative.maxodiff.cli.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.maxodiff.config.MaxoTermMap;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.io.PhenopacketFileParser;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
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

        for (int i = 0; i < phenopacketPaths.size(); i++) {
            try {

                Path pPath = phenopacketPaths.get(i);
                PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(pPath);
                Sample sample = Sample.of(phenopacketData.sampleId(),
                        phenopacketData.presentHpoTermIds().toList(),
                        phenopacketData.excludedHpoTermIds().toList());

                MaxoTermMap maxoTermMap = new MaxoTermMap(maxoDataPath);

                System.out.println(weights);
                System.out.println(nDiseasesList);

                // Run LIRICAL analysis
                LiricalAnalysis liricalAnalysis = new LiricalAnalysis(genomeBuild, TranscriptDatabase.REFSEQ,
                        runConfiguration.pathogenicityThreshold, runConfiguration.defaultVariantBackgroundFrequency, runConfiguration.strict,
                        runConfiguration.globalAnalysisMode, dataSection.liricalDataDirectory, dataSection.exomiserDatabase, vcfPath);

                AnalysisResults results = liricalAnalysis.runLiricalAnalysis(pPath);

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
                HpoDiseases diseases = maxoTermMap.getDiseases();
                Map<TermId, Set<TermId>> fullHpoToMaxoTermIdMap = maxoTermMap.getFullHpoToMaxoTermIdMap(maxoTermMap.getFullHpoToMaxoTermMap());
                MinimalOntology hpo = maxoTermMap.getOntology();

                Map<String, DiffDiagRefiner> refiners = new HashMap<>();
                refiners.put("MaxoDiff", new MaxoDiffRefiner(diseases, fullHpoToMaxoTermIdMap, hpo));
                if (dummy) {
                    refiners.put("Dummy", new DummyDiffDiagRefiner(diseases, fullHpoToMaxoTermIdMap, hpo));
                }

                List<DifferentialDiagnosis> differentialDiagnoses = new LinkedList<>();
                for (TestResult result : results.resultsWithDescendingPostTestProbability().toList()) {
                    differentialDiagnoses.add(DifferentialDiagnosis.of(result.diseaseId(),
                            result.posttestProbability(), result.getCompositeLR()));
                }

                Set<SimpleTerm> allMaxoTerms = maxoTermMap.getFullHpoToMaxoTermMap().values()
                        .stream().flatMap(Collection::stream).collect(Collectors.toSet());
                Map<TermId, String> allMaxoTermsMap = new HashMap<>();
                allMaxoTerms.forEach(st -> allMaxoTermsMap.put(st.tid(), st.label()));

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
                            TermId maxScoreMaxoTermId = TermId.of(topResult.maxoTermScore().maxoId());
                            String maxScoreTermLabel = allMaxoTermsMap.get(maxScoreMaxoTermId);
                            double maxScoreValue = topResult.maxoTermScore().score();

                            System.out.println("Max Score: " + maxScoreMaxoTermId + " (" + maxScoreTermLabel + ")" + " = " + maxScoreValue);

                        }
                        System.out.println("Weight = " + weight);



                        TermId diseaseId = phenopacketData.diseaseIds().get(0);


//                        if (dummy) {
//                            RefinementResults dummyResults = DummyDiffDiagRefiner.run(sample, differentialDiagnoses, options);
//                            String dummyFileName = String.join("_",
//                                    phenopacketName.replace(".json", ""),
//                                    "n" + nDiseases,
//                                    "w" + weight,
//                                    "results_dummy.json");
//                            Path dummyResultsFilePath = Path.of(String.join(File.separator, outputDir.toString(), dummyFileName));
//                            writeToJsonFile(dummyResultsFilePath, dummyResults);
//                            List<MaxodiffResult> dummyResultsList = dummyResults.maxodiffResults().stream().toList();
//                            MaxodiffResult dummyTopResult = dummyResultsList.get(0);
//                            TermId dummyMaxScoreMaxoTermId = TermId.of(dummyTopResult.maxoTermScore().maxoId());
//                            String dummyMaxScoreTermLabel = allMaxoTermsMap.get(dummyMaxScoreMaxoTermId);
//                            double dummyMaxScoreValue = dummyTopResult.maxoTermScore().score();
//
//                            System.out.println("Dummy Max Score: " + dummyMaxScoreMaxoTermId + " (" + dummyMaxScoreTermLabel + ")" + " = " + dummyMaxScoreValue);
//                        }
                    }
                }
            } catch (Exception ex) {
                LOGGER.info(ex.getMessage());
            }
        }

        return 0;
    }


    public void writeToJsonFile(Path filePath, RefinementResults results) throws IOException {
        ObjectWriter writer = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();
        writer.writeValue(new File(filePath.toString()), results);
    }


}
