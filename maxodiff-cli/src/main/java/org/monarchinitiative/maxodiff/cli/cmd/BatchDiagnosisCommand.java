package org.monarchinitiative.maxodiff.cli.cmd;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.maxodiff.config.MaxoDiffLoader;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.config.MaxodiffPropsConfiguration;
import org.monarchinitiative.maxodiff.core.MaxoDiffAnalysisResultRow;
import org.monarchinitiative.maxodiff.core.MaxodiffAnalysisRunner;
import org.monarchinitiative.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.PhenopacketData;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.phenomizer.IcMicaData;
import org.monarchinitiative.maxodiff.phenomizer.PhenomizerDifferentialDiagnosisEngine;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static org.monarchinitiative.maxodiff.cli.cmd.DifferentialDiagnosisCommand.openOutputFileWriter;


/**
 * Perform Differential Diagnosis calculations
 */

@CommandLine.Command(name = "batch", aliases = {"b"},
        mixinStandardHelpOptions = true,
        description = "batch maxodiff analysis")
public class BatchDiagnosisCommand extends BaseCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DifferentialDiagnosisCommand.class);

    @CommandLine.Option(names = {"-B", "--batchDir"},
            description = "Path to directory containing phenopackets.")
    protected String batchDir;

    @CommandLine.Option(names = {"-N", "--nDiseasesList"},
            split=",",
            arity = "1..*",
            description = "Comma-separated list of n diseases to include in differential diagnosis.")
    protected List<Integer> nDiseasesArg = List.of(20);

    @CommandLine.Option(names = {"-NR", "--nRepetitionsList"},
            split=",",
            arity = "1..*",
            description = "Comma-separated list of n repetitions to include in differential diagnosis.")
    protected List<Integer> nRepetitionsArg = List.of(100);

    @CommandLine.Option(
            names = {"-m", "--maxoData"},
            description = "Path to maxo data directory (default: ${DEFAULT-VALUE}).")
    protected Path maxoDataPath = Path.of("data");

    @CommandLine.Option(names = {"-O", "--outputDirectory"},
            description = "Where to write the results files (default: ${DEFAULT-VALUE}).")
    protected Path outputDir = Path.of(".");

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
        MaxoDiffLoader mdloader =  MaxoDiffLoader.fileLoader(maxoDataPath);
        HpoDiseases hpoDiseases = mdloader.hpoDiseases();
        IcMicaData icMicaData = mdloader.icMicaData();
        MaxodiffDataResolver maxodiffDataResolver = MaxodiffDataResolver.of(maxoDataPath);
        MaxodiffPropsConfiguration maxodiffPropsConfiguration = MaxodiffPropsConfiguration.createConfig(maxodiffDataResolver);

        DiffDiagRefiner maxoDiffRefiner = maxodiffPropsConfiguration.diffDiagRefiner("score");
        BiometadataService biometadataService = maxodiffPropsConfiguration.biometadataService();
        Map<TermPair, Double> icMicaDict = icMicaData.icMicaDict();
        DifferentialDiagnosisEngine engine = new PhenomizerDifferentialDiagnosisEngine(hpoDiseases, icMicaDict);
        Map<String, MaxoDiffAnalysisResultRow> resultsMap = new HashMap<>();
        try (BufferedWriter writer = openOutputFileWriter(maxodiffResultsFilePath);
             CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord(MaxoDiffAnalysisResultRow.headerFields());
            int nTotal = phenopacketPaths.size() * nRepetitionsArg.size() * nDiseasesList.size();
            int c = 0;
            for (int nDiseases : nDiseasesList) {
                for (int nRepetitions : nRepetitionsList) {
                    MaxodiffAnalysisRunner runner = new MaxodiffAnalysisRunner(nDiseases,
                            nRepetitions,
                            engine,
                            maxoDiffRefiner,
                            biometadataService);
                    for (Path phenopacketPath : phenopacketPaths) {
                        try {
                            String phenopacketFileName = phenopacketPath.toFile().getName();
                            PhenopacketData phenopacketData = PhenopacketData.readPhenopacketData(phenopacketPath);
                            MaxoDiffAnalysisResultRow row = runner.batchAnalysis(phenopacketData);
                            printer.printRecord(row.getFields());
                            c++;
                            updateProgress(c, nTotal);
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage());
                        }
                    }
                }
            }
        }
        return 0;
    }

    private int lastPercent = -1;
    private void updateProgress(int pkt, int total) {
        int percent = (int) ((pkt * 100) / total);
        if (percent != lastPercent) {
            lastPercent = percent;
            int filled = percent / 2; // 50 chars for 100%
            System.out.print("Finished ppkt: [" + "=".repeat(filled) + " ".repeat(50 - filled) + "] " + percent + "%\r");
            if (percent == 100) System.out.println();
        }
    }

}
