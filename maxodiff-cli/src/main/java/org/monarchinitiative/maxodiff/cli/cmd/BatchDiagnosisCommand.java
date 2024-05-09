package org.monarchinitiative.maxodiff.cli.cmd;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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

@CommandLine.Command(name = "batch", aliases = {"b"},
        mixinStandardHelpOptions = true,
        description = "batch maxodiff analysis")
public class BatchDiagnosisCommand extends DifferentialDiagnosisCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DifferentialDiagnosisCommand.class);

    @CommandLine.Option(names = {"-P", "--phenopacketBatchDir"},
            description = "Path to directory containing phenopackets.")
    protected String phenopacketBatchDir;

    @CommandLine.Option(names = {"-R", "--liricalBatchDir"},
            description = "Path to directory containing LIRICAL TSV results files.")
    protected String liricalBatchDir;

    @CommandLine.Option(names = {"-W", "--weights"},
            split=",",
            arity = "1..*",
            description = "Comma-separated list of weight values to use in final score calculation.")
    public List<Double> weightsArg;

    @CommandLine.Option(names = {"-T", "--thresholds"},
//            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of posttest probability thresholds for filtering diseases to include in differential diagnosis.")
    protected List<Double> thresholdsArg;

    public static Map<String, List<Object>> resultsMap;


    @Override
    public Integer call() throws Exception {

        Map<Path, Path> phenopacketLiricalPathMap = new HashMap<>();
        if (phenopacketBatchDir != null) {
            File phenopacketFolder = new File(phenopacketBatchDir);
            List<File> phenopacketFiles = Arrays.asList(Objects.requireNonNull(phenopacketFolder.listFiles()));
            Collections.sort(phenopacketFiles);
            File liricalFolder = new File(liricalBatchDir);
            List<File> liricalFiles = Arrays.asList(Objects.requireNonNull(liricalFolder.listFiles()));
            for (File phenopacketFile : phenopacketFiles) {
                BasicFileAttributes basicFileAttributes = Files.readAttributes(phenopacketFile.toPath(), BasicFileAttributes.class);
                if (basicFileAttributes.isRegularFile() && !basicFileAttributes.isDirectory() && !phenopacketFile.getName().startsWith(".")) {
                    for (File liricalFile : liricalFiles) {
                        String liricalFileName = liricalFile.getName();
                        String phenopacketFileName = phenopacketFile.getName();
                        if (liricalFileName.contains(phenopacketFileName.substring(0, phenopacketFileName.length()-5))) {
                            phenopacketLiricalPathMap.put(phenopacketFile.toPath(), liricalFile.toPath());
                        }
                    }
                }
            }
        }

//        System.out.println(phenopacketLiricalPathMap);

        Path maxodiffResultsFilePath = Path.of(String.join(File.separator, outputDir.toString(), "maxodiff_results.csv"));

        try (BufferedWriter writer = openWriter(maxodiffResultsFilePath); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord("phenopacket", "lirical_results_file", "disease_id", "maxo_id", "maxo_label",
                    "filter_posttest_prob", "n_diseases", "disease_ids", "weight", "score"); // header

            for (Map.Entry<Path, Path> entry : phenopacketLiricalPathMap.entrySet()) {
                String thresholds = thresholdsArg.stream().map(Object::toString).collect(Collectors.joining(","));
                String weights = weightsArg.stream().map(Object::toString).collect(Collectors.joining(","));
                try {
                    DifferentialDiagnosisCommand differentialDiagnosisCommand = new DifferentialDiagnosisCommand();
                    CommandLine.call(differentialDiagnosisCommand,
                            "-m", maxoDataPath.toString(),
                            "-p", entry.getKey().toString(),
                            "-r", entry.getValue().toString(),
                            "-t", thresholds,
                            "-w", weights,
                            "-O", outputDir.toString());


                    Map<String, List<Object>> resultsMap = getResultsMap();

//                    System.out.println("BatchCmd resultsMap = " + resultsMap);

                    List<Object> phenopacketNames = resultsMap.get("phenopacketName");
                    List<Object> liricalOutputNames = resultsMap.get("liricalOutputName");
                    List<Object> diseaseIdList = resultsMap.get("diseaseId");
                    List<Object> maxScoreMaxoTermIds = resultsMap.get("maxScoreMaxoTermId");
                    List<Object> maxScoreTermLabels = resultsMap.get("maxScoreTermLabel");
                    List<Object> posttestFilters = resultsMap.get("threshold");
                    List<Object> topNDiseasesList = resultsMap.get("topNDiseases");
                    List<Object> diseaseIdsList = resultsMap.get("diseaseIds");
                    List<Object> weightList = resultsMap.get("weight");
                    List<Object> maxScoreValues = resultsMap.get("maxScoreValue");
                    for (int j = 0; j < phenopacketNames.size(); j++) {
                        String phenopacketName = phenopacketNames.get(j).toString();
                        String liricalOutputName = liricalOutputNames.get(j).toString();
                        TermId diseaseId = TermId.of(diseaseIdList.get(j).toString());
                        TermId maxScoreMaxoTermId = TermId.of(maxScoreMaxoTermIds.get(j).toString());
                        String maxScoreTermLabel = maxScoreTermLabels.get(j).toString();
                        double posttestFilter = Double.parseDouble(posttestFilters.get(j).toString());
                        int topNDiseases = Integer.parseInt(topNDiseasesList.get(j).toString());
                        String diseaseIds = diseaseIdsList.get(j).toString();
                        double weight = Double.parseDouble(weightList.get(j).toString());
                        double maxScoreValue = Double.parseDouble(maxScoreValues.get(j).toString());

                        writeResults(phenopacketName, liricalOutputName, diseaseId, maxScoreMaxoTermId, maxScoreTermLabel,
                                posttestFilter, topNDiseases, diseaseIds, weight, maxScoreValue, printer);
                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                    continue;
                }
            }
        }

        return 0;
    }


    protected static void setResultsMap(Map<String, List<Object>> results) {
        resultsMap = results;
    }


    protected Map<String, List<Object>> getResultsMap() {
        return resultsMap;
    }

    private static BufferedWriter openWriter(Path outputPath) throws IOException {
        return outputPath.toFile().getName().endsWith(".gz")
                ? new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(outputPath))))
                : Files.newBufferedWriter(outputPath);
    }



    /**
     * Write results of a single benchmark into the provided {@code printer}.
     */
    private static void writeResults(String phenopacketName,
                                     String liricalOutputName,
                                     TermId diseaseId,
                                     TermId maxoId,
                                     String maxoLabel,
                                     double topPosttestProb,
                                     int topNdiseases,
                                     String diseaseIds,
                                     double weight,
                                     double score,
                                     CSVPrinter printer) {

        try {
            printer.print(phenopacketName);
            printer.print(liricalOutputName);
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
