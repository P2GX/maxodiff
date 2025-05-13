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

    @CommandLine.Option(names = {"-N", "--nDiseasesList"},
//            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of n diseases to include in differential diagnosis.")
    protected List<Integer> nDiseasesArg;
    @CommandLine.Option(names = {"-NR", "--nRepetitionsList"},
//            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of n repetitions to include in differential diagnosis.")
    protected List<Integer> nRepetitionsArg;

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

        List<Integer> nDiseasesList = new ArrayList<>();
        nDiseasesArg.forEach(nDiseasesList::add);
        List<Integer> nRepetitionsList = new ArrayList<>();
        nRepetitionsArg.forEach(nRepetitionsList::add);

        Path maxodiffResultsFilePath = Path.of(String.join(File.separator, outputDir.toString(), "maxodiff_results.csv"));

        try (BufferedWriter writer = openOutputFileWriter(maxodiffResultsFilePath); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord("phenopacket", "disease_id", "maxo_id", "maxo_label",
                    "n_diseases", "disease_ids", "n_repetitions", "weight", "score"); // header

            for (Path phenopacketPath : phenopacketPaths) {
                for (int nDiseases : nDiseasesList) {
                    for (int nRepetitions : nRepetitionsList) {
                        try {

                            String phenopacketFileName = phenopacketPath.toFile().getName();

                            runSingleMaxodiffAnalysis(phenopacketPath, phenopacketFileName, nDiseases, nRepetitions, false, printer);


                            Map<String, List<Object>> resultsMap = getResultsMap();

                            System.out.println("BatchCmd resultsMap = " + resultsMap);

                            List<Object> phenopacketNames = resultsMap.get("phenopacketName");
                            List<Object> diseaseIdList = resultsMap.get("diseaseId");
                            List<Object> maxScoreMaxoTermIds = resultsMap.get("maxScoreMaxoTermId");
                            List<Object> maxScoreTermLabels = resultsMap.get("maxScoreTermLabel");
                            List<Object> topNDiseasesList = resultsMap.get("topNDiseases");
                            List<Object> diseaseIdsList = resultsMap.get("diseaseIds");
                            List<Object> nRepList = resultsMap.get("nRepetitions");
                            List<Object> maxScoreValues = resultsMap.get("maxScoreValue");
                            for (int j = 0; j < phenopacketNames.size(); j++) {
                                String phenopacketName = phenopacketNames.get(j).toString();
                                TermId diseaseId = TermId.of(diseaseIdList.get(j).toString());
                                TermId maxScoreMaxoTermId = TermId.of(maxScoreMaxoTermIds.get(j).toString());
                                String maxScoreTermLabel = maxScoreTermLabels.get(j).toString();
                                int topNDiseases = Integer.parseInt(topNDiseasesList.get(j).toString());
                                String diseaseIds = diseaseIdsList.get(j).toString();
                                int nRepetitionsValue = Integer.parseInt(nRepList.get(j).toString());
                                double maxScoreValue = Double.parseDouble(maxScoreValues.get(j).toString());

                                writeResults(phenopacketName, diseaseId, maxScoreMaxoTermId, maxScoreTermLabel,
                                        topNDiseases, diseaseIds, nRepetitionsValue, maxScoreValue, printer);
                            }
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage());
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



}
