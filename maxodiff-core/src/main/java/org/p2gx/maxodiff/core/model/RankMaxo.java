package org.p2gx.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.p2gx.maxodiff.core.JpsChecker;
import org.p2gx.maxodiff.core.ProgessBar;

import org.p2gx.maxodiff.core.analysis.*;
import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.ontology.data.TermId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RankMaxo {
    private final static Logger LOGGER = LoggerFactory.getLogger(RankMaxo.class);
    private final Map<TermId, Set<TermId>> maxoToHpoTermIdMap;
    private final MaxoHpoTermProbabilities maxoHpoTermProbabilities;
    private final DDxEngine engine;
    double progress;
    RankMaxoProgress rankMaxoProgress;
    private final List<DifferentialDiagnosis> allInitialDiagnoses;
    private final List<DifferentialDiagnosis> initialDiagnoses;
    private final List<HpoFrequency> hpoFrequenciesNDiseases;

    /**
     * The {@code RankMaxo} ranks MAxO terms and returns results in descending order by score.
     *
     * @param maxoToHpoTermIdMap Map of MAxO term ids : Set of corresponding HPO term ids.
     * @param maxoHpoTermProbabilities Class with methods for MAxO:HPO term probability calculations
     *                                 (e.g. Probability that the HPO term will be ascertained by a MAxO term procedure).
     * @param engine The engine used for the differential diagnosis. The default is Phenomizer.
     * @param hpoFrequenciesNDiseases List of HpoFrequency objects for top N diseases.
     * @param allInitialDiagnoses Full list of diseases from the initial differential diagnosis.
     *
     * @author Martha Beckwith
     * @since 1.0
     */
    public RankMaxo(Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                    MaxoHpoTermProbabilities maxoHpoTermProbabilities,
                    DDxEngine engine,
                    MinimalOntology hpo,
                    List<DifferentialDiagnosis> allInitialDiagnoses,
                    List<DifferentialDiagnosis> initialDiagnoses,
                    List<HpoFrequency> hpoFrequenciesNDiseases) {
        this.maxoToHpoTermIdMap = maxoToHpoTermIdMap;
        this.maxoHpoTermProbabilities = maxoHpoTermProbabilities;
        this.engine = engine;
        this.allInitialDiagnoses = allInitialDiagnoses;
        this.initialDiagnoses = initialDiagnoses;
        this.hpoFrequenciesNDiseases = hpoFrequenciesNDiseases;
    }

    /**
     *
     * @param ppkt Input phenopacket with present and excluded HPO terms.
     * @param diseaseIds Set of top n OMIM disease Ids to use for analysis.
     * @return Map of MAxO scores sorted in descending order by score
     */
    public List<RankedMaxoResult> rankMaxoTerms(PhenopacketData ppkt,
                                                Set<TermId> diseaseIds,
                                                MdContext context) throws Exception {

        int nRepetitions = context.params().nRepetitions();
        BiometadataService biometadataService = context.biometadataService();
        List<TermId> ppktMaxoIds = ppkt.maxoProcedureIds();
        AssessablePhenotypes ascertainablePhenotypes = new AssessablePhenotypes(maxoHpoTermProbabilities.getHpoDiseases());
        Map<TermId, Set<TermId>> fullMaxoToHpoTermIdMap = maxoHpoTermProbabilities.getMaxoToHpoTermIdMap();
//        Map<TermId, Set<TermId>> fullMaxoToHpoTermIdMap = MaxoHpoTermIdMaps.getMaxoToHpoTermIdMap(context.resources().maxoAnnotsMap());

        int numThreads = Runtime.getRuntime().availableProcessors() - 1;
        LOGGER.info("Making ExecutorService using " + numThreads + " threads.");
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicInteger completedTasks = new AtomicInteger(0);
        List<Callable<RankedMaxoResult>> tasks = new ArrayList<>();
        int maxoIdx = 0;
        ProgessBar pb = new ProgessBar(maxoIdx, maxoToHpoTermIdMap.size());
        for (TermId maxoId : maxoToHpoTermIdMap.keySet()) {
            LOGGER.debug("MAxO Id = " + maxoId);
            if (ppktMaxoIds.contains(maxoId)) {
                LOGGER.debug("Sample {}  already contains {}.", ppkt.sampleId(), maxoId);
                continue;
            }
            LOGGER.debug("Making MaxoHpoDiseaseRank Object");
            MaxoHpoDiseaseRank maxoHpoDiseaseRank = MaxoHpoDiseaseRank.Builder.builder()
                    .initialDiagnoses(allInitialDiagnoses)
                    .ascertainablePhenotypes(ascertainablePhenotypes)
                    .maxoToHpoTermIdMap(fullMaxoToHpoTermIdMap)
                    .maxoId(maxoId)
                    .sample(ppkt)
                    .nDiagnoses(500)
                    .maxoLabel(biometadataService.maxoLabel(maxoId.getValue()).get())
                    .build();
            LOGGER.debug("Making RankMaxoProgressObject");
            rankMaxoProgress = new RankMaxoProgress(maxoToHpoTermIdMap.size());
            int finalMaxoIdx = maxoIdx;
            tasks.add(() -> {
                MaxoTermEvaluator evaluateMaxoTerm = new MaxoTermEvaluator(maxoHpoDiseaseRank, nRepetitions, ppkt,
                        engine, maxoHpoTermProbabilities,
                        initialDiagnoses, diseaseIds, biometadataService, hpoFrequenciesNDiseases);
                double done = completedTasks.incrementAndGet();
                rankMaxoProgress.updateProgress(maxoId.getValue(), done);
                if (JpsChecker.isMainClassRunning("org.p2gx.maxodiff.cli.Main")) {
                    pb.print(finalMaxoIdx);
                }
                return evaluateMaxoTerm.call();
            });
            maxoIdx++;
        }

        List<Future<RankedMaxoResult>> futures = executor.invokeAll(tasks);

        List<RankedMaxoResult> results = new ArrayList<>();
        for (Future<RankedMaxoResult> future : futures) {
            try {
                results.add(future.get()); // blocks until the result is available
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error(e.getMessage());
            }
        }
        executor.shutdown();

        return results.stream()
                .sorted(Comparator.comparing(RankedMaxoResult :: maxoScore).reversed())
                .toList();
    }

    public List<RankedMaxoResultSingleDisease> getDiseaseBestMaxoTerms(PhenopacketData ppkt,
                                                                      TermId targetDiseaseId,
                                                                      Set<TermId> diseaseIds,
                                                                      MdContext context) throws InterruptedException {

        int nRepetitions = context.params().nRepetitions();
        BiometadataService biometadataService = context.biometadataService();
        List<TermId> ppktMaxoIds = ppkt.maxoProcedureIds();
        Map<TermId, Set<TermId>> fullMaxoToHpoTermIdMap = maxoHpoTermProbabilities.getMaxoToHpoTermIdMap();

        int numThreads = Runtime.getRuntime().availableProcessors() - 1;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicInteger completedTasks = new AtomicInteger(0);
        List<Callable<RankedMaxoResultSingleDisease>> tasks = new ArrayList<>();
        int maxoIdx = 0;
        ProgessBar pb = new ProgessBar(maxoIdx, maxoToHpoTermIdMap.size());
        for (TermId maxoId : maxoToHpoTermIdMap.keySet()) {
            if (ppktMaxoIds.contains(maxoId)) {
                LOGGER.debug("Sample {}  already contains {}.", ppkt.sampleId(), maxoId);
                continue;
            }

            AssessablePhenotypes ascertainablePhenotypes =
                    new AssessablePhenotypes(maxoHpoTermProbabilities.getHpoDiseases());
            MaxoHpoDiseaseRank maxoHpoDiseaseRank = MaxoHpoDiseaseRank.Builder.builder()
                    .initialDiagnoses(allInitialDiagnoses)
                    .ascertainablePhenotypes(ascertainablePhenotypes)
                    .maxoToHpoTermIdMap(fullMaxoToHpoTermIdMap)
                    .maxoId(maxoId)
                    .sample(ppkt)
                    .nDiagnoses(500)
                    .maxoLabel(biometadataService.maxoLabel(maxoId.getValue()).get())
                    .build();
            rankMaxoProgress = new RankMaxoProgress(maxoToHpoTermIdMap.size());
            int finalMaxoIdx = maxoIdx;
            tasks.add(() -> {
                MaxoTermEvaluatorSingleDisease evaluateMaxoTerm = new MaxoTermEvaluatorSingleDisease(maxoHpoDiseaseRank, nRepetitions, ppkt,
                        engine, maxoHpoTermProbabilities,
                        initialDiagnoses, diseaseIds, biometadataService, context, targetDiseaseId);
                double done = completedTasks.incrementAndGet();
                rankMaxoProgress.updateProgress(maxoId.getValue(), done);
                if (JpsChecker.isMainClassRunning("org.p2gx.maxodiff.cli.Main")) {
                    pb.print(finalMaxoIdx);
                }
                return evaluateMaxoTerm.call();
            });
            maxoIdx++;
        }

        List<Future<RankedMaxoResultSingleDisease>> futures = executor.invokeAll(tasks);

        List<RankedMaxoResultSingleDisease> results = new ArrayList<>();
        for (Future<RankedMaxoResultSingleDisease> future : futures) {
            try {
                results.add(future.get()); // blocks until the result is available
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error(e.getMessage());
            }
        }
        executor.shutdown();


        return results.stream()
                .sorted(Comparator.comparing(RankedMaxoResultSingleDisease :: totalIC).reversed())
                .toList();

    }

    private double updateProgress(double p, int nMaxoTermIds) {
        return (p / nMaxoTermIds) * 100.;
    }

    public double getProgress() {
        return progress;
    }

    public RankMaxoProgress getRankMaxoProgress() {
        return rankMaxoProgress;
    }

}
