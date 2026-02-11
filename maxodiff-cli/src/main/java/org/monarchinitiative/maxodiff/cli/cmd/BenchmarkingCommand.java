package org.monarchinitiative.maxodiff.cli.cmd;

import org.monarchinitiative.maxodiff.cli.benchmarking.BaseBenchmarker;
import org.monarchinitiative.maxodiff.cli.benchmarking.BenchmarkProcedure;
import org.monarchinitiative.maxodiff.cli.benchmarking.BenchmarkResult;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.config.MaxodiffPropsConfiguration;
import org.monarchinitiative.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementOptions;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.phenomizer.IcMicaData;
import org.monarchinitiative.maxodiff.phenomizer.IcMicaDictLoader;
import org.monarchinitiative.maxodiff.phenomizer.PhenomizerDifferentialDiagnosisEngine;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
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
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;


/**
 * Perform Differential Diagnosis calculations
 */

@CommandLine.Command(name = "benchmarking", aliases = {"BX"},
        mixinStandardHelpOptions = true,
        description = "benchmark maxodiff analysis")
public class BenchmarkingCommand extends DifferentialDiagnosisCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(BenchmarkingCommand.class);


    @CommandLine.Option(names = {"-ND", "--NDiseases"},
            description = "Number of diseases to include in differential diagnosis.")
    protected int nDiseases = 20;

    @CommandLine.Option(names = {"-NR", "--NRepetitions"},
            description = "Numbers of repetitions for running differential diagnosis.")
    protected int nRepetitions = 80;

    @CommandLine.Option(names = {"-o", "--outputFilename"},
            description = "Filename of the benchmark results CSV file. The CSV is compressed if the path has the '.gz' suffix (if not set, do not write output)")
    private Path outputName = Path.of("benchmark_results.csv");

    @CommandLine.Option(names = {"--ppktdir"},
            description = "Directory with phenopackets for benchmarking")
    private Path ppktDir = null;


    private DifferentialDiagnosisEngine phenomizer;

    private MaxodiffPropsConfiguration maxodiffPropsConfiguration ;
    private DiffDiagRefiner refiner;
    private HpoDiseases hpoDiseases;
    private RefinementOptions refinementOptions;

    @Override
    public Integer execute() throws Exception {

        MaxodiffDataResolver maxodiffDataResolver = MaxodiffDataResolver.of(maxoDataPath);
        this.maxodiffPropsConfiguration = MaxodiffPropsConfiguration.createConfig(maxodiffDataResolver);
        this.refiner = maxodiffPropsConfiguration.diffDiagRefiner();

        Ontology ontology = OntologyLoader.loadOntology(MaxodiffDataResolver.of(maxoDataPath).hpoJson().toFile());
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(ontology, HpoDiseaseLoaderOptions.defaultOmim());
        Path hpoaPath = MaxodiffDataResolver.of(maxoDataPath).phenotypeAnnotations();
        this.hpoDiseases = loader.load(hpoaPath);
        IcMicaData icMicaData = IcMicaDictLoader.loadIcMicaDict(MaxodiffDataResolver.of(maxoDataPath).icMicaDict());
        Map<TermPair, Double> icMicaDict = icMicaData.icMicaDict();
        this.phenomizer = new PhenomizerDifferentialDiagnosisEngine(hpoDiseases, icMicaDict);
        this.refinementOptions = RefinementOptions.of(nDiseases, nRepetitions);

        if (this.phenopacketPath != null && this.phenopacketPath.toFile().isFile()) {
            List<BenchmarkResult> results = runShuffleOnePPkt(this.phenopacketPath);
            outputResultList(results, true, false);
        } else if (this.ppktDir != null && this.ppktDir.toFile().isDirectory()) {
            List<Path> phenopacketPaths = new ArrayList<>();
            File folder = new File(ppktDir.toUri());
            File[] files = folder.listFiles();
            assert files != null;
            for (File file : files) {
                BasicFileAttributes basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                if (basicFileAttributes.isRegularFile() && !basicFileAttributes.isDirectory() && !file.getName().startsWith(".")) {
                    phenopacketPaths.add(file.toPath());
                }
            }

            for (Path ppktPath : phenopacketPaths) {
                int ppktIdx = phenopacketPaths.indexOf(ppktPath);
                int ppktN = ppktIdx + 1;
                int nPpkts = phenopacketPaths.size();
                float percent = (((float) ppktN) / nPpkts) * 100;
                boolean writeHeader = false;
                List<BenchmarkResult> results = runShuffleOnePPkt(ppktPath);
                if (ppktIdx == 0) {
                    writeHeader = true;
                }
                outputResultList(results, writeHeader, true);
                LOGGER.info("Finished {} of {} phenopackets. ({}% complete)", ppktN, nPpkts, percent);
            }
        } else {
            System.err.println("[ERROR] No phenopacket path or directory provided.");
            System.err.println("[ERROR] Provide path to Phenopacket json file using -p/--phenopacket.");
            System.err.println("[ERROR] Provide path to a directory of Phenopacket json files using --ppktdir.");
            return 1;
        }

        return 0;
    }

    private void outputResultList(List<BenchmarkResult> results, boolean writeHeader, boolean append) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(this.outputName.toFile(), append))) {
            if (writeHeader) {
                bw.write(BenchmarkResult.getHeaderLine() + "\n");
            }
            for (BenchmarkResult result : results) {
                bw.write(result.getRow() + "\n");
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


    private List<BenchmarkResult> runSpikeOnePPkt(Path ppktPath) {
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
                BenchmarkResult bres = getSpikedBenchmarkResult(ppktId, i, initialResults, randomizedResults);
                resultList.add(bres);
            }
            LOGGER.info("Finished benchmark of {}.", ppktPath);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return resultList;
    }


    private List<BenchmarkResult> runShuffleOnePPkt(Path ppktPath) {
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
            TermId topMaxo = initialResults.getFirst().rankMaxoScore().maxoId();
            List<Double> topRandomScores = new ArrayList<>();
            int parallelism = 4;
            ForkJoinPool customThreadPool = new ForkJoinPool(parallelism);
            try {
                customThreadPool.submit(() ->
                    IntStream.range(0, 50).parallel().forEach(i -> {
                        List<MaxodiffResult> randomizedResults = null;
                        try {
                            randomizedResults = benchmarker.shuffledRandomizer();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        //TODO: compare total Information Content for Maxo Terms instead of scores
                        List<MaxodiffResult> topResultRandomList = randomizedResults.stream()
                                .filter(mr -> mr.rankMaxoScore().maxoId().equals(topMaxo)).toList();
                        double maxScoreValueRandom = topResultRandomList.isEmpty() ? 0.0 : topResultRandomList.getFirst().rankMaxoScore().maxoScore();
                        topRandomScores.add(maxScoreValueRandom);
                        if (i % 10 == 0) {
                            LOGGER.info("Finished index " + i);
                        }
                    })
                ).get();
            } finally {
                customThreadPool.shutdown();
            }
            double avgTopRandomScore = topRandomScores.stream().mapToDouble(Double::valueOf).average().orElse(0);
            BenchmarkResult bres = getShuffledBenchmarkResult(ppktId, initialResults, avgTopRandomScore);
            resultList.add(bres);
            LOGGER.info("Finished benchmark of {}.", ppktPath);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        return resultList;
    }



    private BenchmarkResult getSpikedBenchmarkResult(String ppktId,
                                                     int spikedIdx,
                                                     List<MaxodiffResult> initialResults,
                                                     List<MaxodiffResult> randomizedResults) {

        TermId topMaxo = initialResults.getFirst().rankMaxoScore().maxoId();
        double maxoFinalScore = initialResults.getFirst().rankMaxoScore().maxoScore();
        List<MaxodiffResult> topResultRandomList = randomizedResults.stream()
                .filter(mr -> mr.rankMaxoScore().maxoId().equals(topMaxo)).toList();
        int topMaxoRandomIdx = topResultRandomList.isEmpty() ? -1 : randomizedResults.indexOf(topResultRandomList.getFirst()) + 1;
        double maxScoreValueRandom = topResultRandomList.isEmpty() ? 0.0 : topResultRandomList.getFirst().rankMaxoScore().maxoScore();
        BenchmarkProcedure procedure = BenchmarkProcedure.SpikedInRandomization;
        int nMaxo = initialResults.size();
        int nMaxoRandom = randomizedResults.size();
        return new BenchmarkResult(ppktId, nDiseases, nRepetitions, topMaxo, maxoFinalScore,
                procedure, topMaxoRandomIdx, maxScoreValueRandom, nMaxo, nMaxoRandom, spikedIdx);
    }

    private BenchmarkResult getShuffledBenchmarkResult(String ppktId,
                                                     List<MaxodiffResult> initialResults,
                                                     double avgTopScoreRandom) {

        TermId topMaxo = initialResults.getFirst().rankMaxoScore().maxoId();
        double maxoFinalScore = initialResults.getFirst().rankMaxoScore().maxoScore();
        BenchmarkProcedure procedure = BenchmarkProcedure.ShuffledRandomization;
        int nMaxo = initialResults.size();
        return new BenchmarkResult(ppktId, nDiseases, nRepetitions, topMaxo, maxoFinalScore,
                procedure, -1, avgTopScoreRandom, nMaxo, -1, -1);
    }










}
