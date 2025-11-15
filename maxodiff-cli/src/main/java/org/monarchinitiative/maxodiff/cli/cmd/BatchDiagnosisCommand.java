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
import org.monarchinitiative.maxodiff.phenomizer.ScoringMode;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
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
            split=",",
            arity = "1..*",
            description = "Comma-separated list of n diseases to include in differential diagnosis.")
    protected List<Integer> nDiseasesArg;
    @CommandLine.Option(names = {"-NR", "--nRepetitionsList"},
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
        MaxoDiffLoader mdloader =  MaxoDiffLoader.fileLoader(maxoDataPath);
        HpoDiseases hpoDiseases = mdloader.hpoDiseases();
        IcMicaData icMicaData = mdloader.icMicaData();
        MaxodiffDataResolver maxodiffDataResolver = MaxodiffDataResolver.of(maxoDataPath);
        MaxodiffPropsConfiguration maxodiffPropsConfiguration = MaxodiffPropsConfiguration.createConfig(maxodiffDataResolver);

        DiffDiagRefiner maxoDiffRefiner = maxodiffPropsConfiguration.diffDiagRefiner("score");
        BiometadataService biometadataService = maxodiffPropsConfiguration.biometadataService();
        Map<TermPair, Double> icMicaDict = icMicaData.icMicaDict();
        DifferentialDiagnosisEngine engine = new PhenomizerDifferentialDiagnosisEngine(hpoDiseases, icMicaDict);
        MaxodiffAnalysisRunner runner = new MaxodiffAnalysisRunner(this.nDiseases,
                this.nRepetitions,
                engine,
                maxoDiffRefiner,
                biometadataService);

        try (BufferedWriter writer = openOutputFileWriter(maxodiffResultsFilePath); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord("phenopacket", "disease_id", "maxo_id", "maxo_label",
                    "n_diseases", "disease_ids", "n_repetitions", "score"); // header
            Map<String, MaxoDiffAnalysisResultRow> resultsMap = new HashMap<>();
            for (Path phenopacketPath : phenopacketPaths) {
                for (int nDiseases : nDiseasesList) {
                    for (int nRepetitions : nRepetitionsList) {
                        try {

                            String phenopacketFileName = phenopacketPath.toFile().getName();
                            ScoringMode scoringMode = ScoringMode.ONE_SIDED;
                            PhenopacketData phenopacketData = PhenopacketData.readPhenopacketData(phenopacketPath);
                            MaxoDiffAnalysisResultRow maxoDiffAnalysisResultRow = runner.batchAnalysis(phenopacketData);
                            resultsMap.put(phenopacketFileName, maxoDiffAnalysisResultRow);
                            System.out.println("BatchCmd resultsMap = " + resultsMap);
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage());
                        }
                    }
                }
            }
            printer.printRecord(MaxoDiffAnalysisResultRow.headerFields());
            for (var row: resultsMap.values()) {
                printer.printRecord(row.getFields());
            }
        }
        return 0;
    }

}
