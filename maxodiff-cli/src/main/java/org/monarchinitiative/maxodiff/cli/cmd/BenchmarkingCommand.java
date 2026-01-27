package org.monarchinitiative.maxodiff.cli.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.maxodiff.cli.benchmarking.BaseBenchmarker;
import org.monarchinitiative.maxodiff.cli.benchmarking.BenchmarkProcedure;
import org.monarchinitiative.maxodiff.cli.benchmarking.BenchmarkResult;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.config.MaxodiffPropsConfiguration;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.MaxoHpoTermIdMaps;
import org.monarchinitiative.maxodiff.core.analysis.RankMaxoScore;
import org.monarchinitiative.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementOptions;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementResults;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.*;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.html.results.HtmlResults;
import org.monarchinitiative.maxodiff.phenomizer.IcMicaData;
import org.monarchinitiative.maxodiff.phenomizer.IcMicaDictLoader;
import org.monarchinitiative.maxodiff.phenomizer.PhenomizerDifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.phenomizer.ScoringMode;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;


/**
 * Perform Differential Diagnosis calculations
 */

@CommandLine.Command(name = "benchmarking", aliases = {"BX"},
        mixinStandardHelpOptions = true,
        description = "benchmark maxodiff analysis")
public class BenchmarkingCommand extends DifferentialDiagnosisCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkingCommand.class);


    @CommandLine.Option(names = {"-ND", "--nDiseases"},
            description = "Number of diseases to include in differential diagnosis.")
    protected int nDiseases = 40;

    @CommandLine.Option(names = {"-NR", "--nRepetitions"},
            description = "Numbers of repetitions for running differential diagnosis.")
    protected int nRepetitions = 80;

    @CommandLine.Option(names = {"-o", "--outputFilename"},
            description = "Filename of the benchmark results CSV file. The CSV is compressed if the path has the '.gz' suffix (if not set, do not write output)")
    private Path outputName = Path.of("benchmark_results.csv");

    @CommandLine.Option(names = {"--ppktdir"},
            description = "Directory with phenopackets for benchmarking")
    private Path ppktDir = null;


    private DifferentialDiagnosisEngine phenomizer;

    private MaxodiffDataResolver maxodiffDataResolver;
    private MaxodiffPropsConfiguration maxodiffPropsConfiguration ;
    private DiffDiagRefiner refiner;
    private HpoDiseases hpoDiseases;
    private RefinementOptions refinementOptions;

    @Override
    public Integer execute() throws Exception {

        Ontology ontology = OntologyLoader.loadOntology(MaxodiffDataResolver.of(maxoDataPath).hpoJson().toFile());
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(ontology, HpoDiseaseLoaderOptions.defaultOmim());
        Path hpoaPath = MaxodiffDataResolver.of(maxoDataPath).phenotypeAnnotations();
        this.hpoDiseases = loader.load(hpoaPath);
        IcMicaData icMicaData = IcMicaDictLoader.loadIcMicaDict(MaxodiffDataResolver.of(maxoDataPath).icMicaDict());
        Map<TermPair, Double> icMicaDict = icMicaData.icMicaDict();
        this.phenomizer = new PhenomizerDifferentialDiagnosisEngine(hpoDiseases, icMicaDict);
        this.refinementOptions = RefinementOptions.of(nDiseases, nRepetitions);

        if (this.phenopacketPath != null || this.phenopacketPath.toFile().isFile()) {
            List<BenchmarkResult> results = runOnePPkt(this.phenopacketPath);
            outputResultList(results);
        } else if (this.ppktDir != null || this.ppktDir.toFile().isDirectory()) {
            System.out.println("IMPLEMENT");
        } else {
            System.err.println("[ERROR] No phenopacket path or directory provided.");
            System.err.println("[ERROR] Provide path to Phenopacket json file using -p/--phenopacket.");
            System.err.println("[ERROR] Provide path to a directory of Phenopacket json files using --ppktdir.");
            return 1;
        }

        // Get phenopacket paths from batch directory, or single phenopacket path if provided instead
        if (this.phenopacketPath == null || !this.phenopacketPath.toFile().exists()) {
            System.err.println("[ERROR] No phenopacket path provided.");
            System.err.println("[ERROR] Provide path to Phenopacket json file using -p/--phenopacket.");
            return 1;
        }

        return 0;
    }

    private void outputResultList(List<BenchmarkResult> results) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(this.outputName.toFile()))) {
            bw.write(BenchmarkResult.getHeaderLine() + "\n");
            for (BenchmarkResult result : results) {
                bw.write(result.getRow() + "\n");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


    private List<BenchmarkResult> runOnePPkt(Path ppktPath) throws Exception {
        List<BenchmarkResult> resultList = new ArrayList<>();
        try {
            BaseBenchmarker benchmarker = new BaseBenchmarker(ppktPath,
                    refinementOptions,
                    this.phenomizer,
                    this.hpoDiseases,
                    maxodiffPropsConfiguration,
                    refiner);
            String ppktId = benchmarker.getSample().id();
            List<MaxodiffResult> initialResults = benchmarker.standardRun();
            for (int i = 0; i < nDiseases; i++) {
                List<MaxodiffResult> randomizedResults = benchmarker.spikedRandomizer(i);
                BenchmarkResult bres = getBenchmarkResult(ppktId, initialResults, randomizedResults);
                resultList.add(bres);
            }
            LOGGER.info("Finished benchmark.");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return resultList;
    }


    /* TODO - ADD STUFF NEEDED FOR BENCHMRKING */
    private BenchmarkResult getBenchmarkResult(String ppktId, List<MaxodiffResult> initialResults, List<MaxodiffResult> randomizedResults) {
        TermId topMaxo = initialResults.getFirst().rankMaxoScore().maxoId();
        double maxoFinalScore = initialResults.getFirst().rankMaxoScore().maxoScore();
        BenchmarkProcedure procedure = BenchmarkProcedure.SpikedInRandomization;
        int nMaxo = initialResults.size();
        return new BenchmarkResult(ppktId, nDiseases, nRepetitions, topMaxo, maxoFinalScore, procedure, nMaxo, );
    }










}
