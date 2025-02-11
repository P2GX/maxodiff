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
import java.util.zip.GZIPOutputStream;


/**
 * Perform Differential Diagnosis calculations
 */

@CommandLine.Command(name = "batch", aliases = {"b"},
        mixinStandardHelpOptions = true,
        description = "batch maxodiff analysis")
public class BatchDiagnosisCommand extends DifferentialDiagnosisCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DifferentialDiagnosisCommand.class);

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

    public static Map<String, List<Object>> resultsMap;


    @Override
    public Integer execute() throws Exception {

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
        }
        Collections.sort(phenopacketPaths);

        List<Double> weights = new ArrayList<>();
        weightsArg.forEach(weights::add);
        List<Integer> nDiseasesList = new ArrayList<>();
        nDiseasesArg.forEach(nDiseasesList::add);

        Path maxodiffResultsFilePath = Path.of(String.join(File.separator, outputDir.toString(), "maxodiff_results.csv"));

        try (BufferedWriter writer = openWriter(maxodiffResultsFilePath); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord("phenopacket", "background_vcf", "disease_id", "maxo_id", "maxo_label",
                    "n_diseases", "disease_ids", "weight", "score"); // header

            for (int i = 0; i < phenopacketPaths.size(); i++) {
                for (int nDiseases : nDiseasesList) {
                    for (double weight : weights) {
//                String nDiseases = nDiseasesArg.stream().map(Object::toString).collect(Collectors.joining(","));
//                String weights = weightsArg.stream().map(Object::toString).collect(Collectors.joining(","));
                        try {
                            DifferentialDiagnosisCommand differentialDiagnosisCommand = new DifferentialDiagnosisCommand();
                            CommandLine.call(differentialDiagnosisCommand,
                                    "-m", maxoDataPath.toString(),
                                    "-d", dataSection.liricalDataDirectory.toString(),
                                    "-p", phenopacketPaths.get(i).toString(),
//                            "-e", dataSection.exomiserDatabase.toString(),
//                            "--vcf", vcfPath.toString(),
//                                    "--assembly", genomeBuild.toString(),
                                    "-n", String.valueOf(nDiseases),
                                    "-w", String.valueOf(weight),
                                    "-O", outputDir.toString());


                            Map<String, List<Object>> resultsMap = getResultsMap();

                            System.out.println("BatchCmd resultsMap = " + resultsMap);

                            List<Object> phenopacketNames = resultsMap.get("phenopacketName");
                            List<Object> backgroundVcfs = resultsMap.get("backgroundVcf");
                            List<Object> diseaseIdList = resultsMap.get("diseaseId");
                            List<Object> maxScoreMaxoTermIds = resultsMap.get("maxScoreMaxoTermId");
                            List<Object> maxScoreTermLabels = resultsMap.get("maxScoreTermLabel");
                            List<Object> topNDiseasesList = resultsMap.get("topNDiseases");
                            List<Object> diseaseIdsList = resultsMap.get("diseaseIds");
                            List<Object> weightList = resultsMap.get("weight");
                            List<Object> maxScoreValues = resultsMap.get("maxScoreValue");
                            for (int j = 0; j < phenopacketNames.size(); j++) {
                                String phenopacketName = phenopacketNames.get(j).toString();
                                String backgroundVcf = backgroundVcfs.get(j).toString();
                                TermId diseaseId = TermId.of(diseaseIdList.get(j).toString());
                                TermId maxScoreMaxoTermId = TermId.of(maxScoreMaxoTermIds.get(j).toString());
                                String maxScoreTermLabel = maxScoreTermLabels.get(j).toString();
                                int topNDiseases = Integer.parseInt(topNDiseasesList.get(j).toString());
                                String diseaseIds = diseaseIdsList.get(j).toString();
                                double weightValue = Double.parseDouble(weightList.get(j).toString());
                                double maxScoreValue = Double.parseDouble(maxScoreValues.get(j).toString());

                                writeResults(phenopacketName, backgroundVcf, diseaseId, maxScoreMaxoTermId, maxScoreTermLabel,
                                        topNDiseases, diseaseIds, weightValue, maxScoreValue, printer);
                            }
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage());
                            continue;
                        }
                    }
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
                                     String backgroundVcfName,
                                     TermId diseaseId,
                                     TermId maxoId,
                                     String maxoLabel,
                                     int topNdiseases,
                                     String diseaseIds,
                                     double weight,
                                     double score,
                                     CSVPrinter printer) {

        try {
            printer.print(phenopacketName);
            printer.print(backgroundVcfName);
            printer.print(diseaseId);
            printer.print(maxoId);
            printer.print(maxoLabel);
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
