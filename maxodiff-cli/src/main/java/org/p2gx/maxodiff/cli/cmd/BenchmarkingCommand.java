package org.p2gx.maxodiff.cli.cmd;

import org.p2gx.maxodiff.cli.benchmarking.BaseBenchmarker;
import org.p2gx.maxodiff.cli.benchmarking.BenchmarkProcedure;
import org.p2gx.maxodiff.cli.benchmarking.BenchmarkResult;
import org.p2gx.maxodiff.core.analysis.*;
import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.io.impl.MdContextBuilder;
import org.p2gx.maxodiff.core.model.PhenopacketData;
import org.p2gx.maxodiff.core.phenomizer.PhenomizerDDxEngine;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;


/**
 * Perform Differential Diagnosis calculations
 */

@CommandLine.Command(name = "benchmarking", aliases = {"BX"},
        mixinStandardHelpOptions = true,
        description = "benchmark maxodiff analysis")
public class BenchmarkingCommand extends DDxCommand {
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

    private DDxEngine phenomizer;
    private MdContext context;
    private List<HpoFrequency> hpoFrequencies;

    @Override
    public Integer execute() throws Exception {

        context = MdContextBuilder.buildContext(
                this.maxoDataPath,
                this.nRepetitions,
                this.nDiseases,
                true);

        hpoFrequencies = context.createHpoFrequencies();

        this.phenomizer = new PhenomizerDDxEngine(context);
        Map<TermId, Double> termToIcMap = context.resources().termToIcMap();

        if (this.phenopacketPath != null && this.phenopacketPath.toFile().isFile()) {
            List<BenchmarkResult> results = runShuffleOnePPkt(this.phenopacketPath, termToIcMap);
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
                List<BenchmarkResult> results = runShuffleOnePPkt(ppktPath, termToIcMap);
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

    private List<BenchmarkResult> runShuffleOnePPkt(Path ppktPath, Map<TermId, Double> termToIcMap) {
        List<BenchmarkResult> resultList = new ArrayList<>();
        try {
            PhenopacketData ppktData = PhenopacketData.readPhenopacketData(ppktPath);
            BiometadataService biometadataService = context.biometadataService();
            BaseBenchmarker benchmarker = new BaseBenchmarker(ppktData,
                    context,
                    this.phenomizer,
                    hpoFrequencies);
            String ppktId = benchmarker.getSample().sampleId();
            List<RankedMaxoResult> initialResults = benchmarker.standardRun(biometadataService);
            TermId topMaxo = initialResults.getFirst().maxoTerm().tid();
            List<Double> topMaxoScoresRandom = Collections.synchronizedList(new ArrayList<>());
            List<Double> icSumsRandom = Collections.synchronizedList(new ArrayList<>());
            int parallelism = 8;
            try (ForkJoinPool customThreadPool = new ForkJoinPool(parallelism)) {
                customThreadPool.submit(() ->
                        IntStream.range(0, 50).parallel().forEach(i -> {
                            try {
                                long seed = 1000L; //10L; //i * 10L;
                                List<RankedMaxoResult> randomizedResults = benchmarker.shuffledRandomizer(seed);

                                List<RankedMaxoResult> topResultRandomList = randomizedResults.stream()
                                        .filter(mr -> mr.maxoTerm().tid().equals(topMaxo))
                                        .toList();

                                List<CountedHpoTerm> ctHpoTerms = topResultRandomList.isEmpty() ?
                                        new ArrayList<>() : topResultRandomList.getFirst().hpoTermIds();

                                List<TermId> discHpoIds = ctHpoTerms.stream()
                                        .map(ctTerm -> ctTerm.hpoTerm().tid())
                                        .toList();

                                double maxoScoreTopResultRandom = topResultRandomList.isEmpty() ? -1.0 :
                                        topResultRandomList.getFirst().maxoScore();

                                double icSumRandom = discHpoIds.stream()
                                        .mapToDouble(id -> termToIcMap.getOrDefault(id, 0.0))
                                        .sum();

                                topMaxoScoresRandom.add(maxoScoreTopResultRandom);
                                icSumsRandom.add(icSumRandom);

                                if (i % 10 == 0) {
                                    LOGGER.info("Finished index {}", i);
                                }
                            } catch (Exception e) {
                                // Ensure exceptions inside the parallel stream are logged/handled
                                LOGGER.error("Error at index {}", i, e);
                                throw new RuntimeException(e);
                            }
                        })
                ).get(); // .get() waits for the submission to complete
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("Parallel execution failed", e);
                Thread.currentThread().interrupt();
            }
            System.out.println(icSumsRandom);
            double avgTopMaxoScoreRandom = topMaxoScoresRandom.stream().mapToDouble(Double::valueOf).average().orElse(0);
            double avgIcSumRandom = icSumsRandom.stream().mapToDouble(Double::valueOf).average().orElse(0);
            BenchmarkResult bres = getShuffledBenchmarkResult(ppktId, initialResults, avgTopMaxoScoreRandom,
                    avgIcSumRandom, termToIcMap);
            resultList.add(bres);
            LOGGER.info("Finished benchmark of {}.", ppktPath);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
        }
        return resultList;
    }


    private BenchmarkResult getShuffledBenchmarkResult(String ppktId,
                                                       List<RankedMaxoResult> initialResults,
                                                       double avgTopScoreRandom,
                                                       double avgIcSumRandom,
                                                       Map<TermId, Double> termToIcMap) {

        TermId topMaxo = initialResults.getFirst().maxoTerm().tid();
        double maxoFinalScore = initialResults.getFirst().maxoScore();
        List<Integer> nDiscoverablePhenotypes = new ArrayList<>();
        List<TermId> topDiscHpoIds = new ArrayList<>();
        double topMaxoIcSum = 0.0;
        for (int i=0; i<10; i++) {
            List<CountedHpoTerm> ctHpoTerms = (initialResults.isEmpty() | initialResults.get(i) == null) ?
                    new ArrayList<>() : initialResults.get(i).hpoTermIds();
            List<TermId> discHpoIds = ctHpoTerms.isEmpty() ? new ArrayList<>() :
                    ctHpoTerms.stream().map(ctTerm -> ctTerm.hpoTerm().tid()).toList();
            nDiscoverablePhenotypes.add(discHpoIds.size());
            if (i == 0 ) {
                topDiscHpoIds = discHpoIds;
                topMaxoIcSum = discHpoIds.isEmpty() ? 0.0 :
                        discHpoIds.stream().mapToDouble(termToIcMap::get).sum();
            }
        }

//
//        double topMaxoIcSum = discHpoIds.isEmpty() ? 0.0 :
//                discHpoIds.stream().mapToDouble(termToIcMap::get).sum();
        LOGGER.info("standard run results:");
        LOGGER.info("initial " + initialResults.getFirst().maxoTerm());
        LOGGER.info("initial " + initialResults.getFirst().rankedOmimTermList());
        LOGGER.info("{} IC Sum = {}.", topDiscHpoIds, topMaxoIcSum);
        BenchmarkProcedure procedure = BenchmarkProcedure.ShuffledRandomization;
        int nMaxo = initialResults.size();
        return new BenchmarkResult(ppktId, nDiseases, nRepetitions, topMaxo, maxoFinalScore,
                procedure, -1, avgTopScoreRandom, nMaxo, -1,
                nDiscoverablePhenotypes, topMaxoIcSum, avgIcSumRandom,-1);
    }

    private Map<TermId, Double> getTermToIcMap(File icFile) {
        Map<TermId, Double> termToIcMap = new HashMap<>();
        try (
            FileInputStream fis = new FileInputStream(icFile);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr)
        ) {
            String line;
            // Read lines until readLine() returns null (end of stream)
            while ((line = br.readLine()) != null) {
                if (line.startsWith("HP:")) {
                    String[] split = line.split(",");
                    TermId tid = TermId.of(split[0]);
                    Double ic = Double.parseDouble(split[1]);
                    termToIcMap.put(tid, ic);
                }

            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        return termToIcMap;
    }










}
