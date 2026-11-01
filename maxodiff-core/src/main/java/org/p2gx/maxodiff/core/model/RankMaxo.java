package org.p2gx.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.p2gx.maxodiff.core.MaxodiffRuntimeException;


import org.p2gx.maxodiff.core.analysis.*;
import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.ontology.data.TermId;

import org.p2gx.maxodiff.core.service.ProgressListener;
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
    private final ProgressListener progressListener;

    /**
     * Number of top-ranked diseases from the initial differential diagnosis whose phenotypes feed the simulation.
     * This is a hyperparameter. The first {@code nDiseases} of them (20 by default) are the ones that are scored;
     * the rest widen the pool from which phenotypes are sampled. A disease at rank r contributes weight 1/r, so
     * the tail from rank 21 to 500 has roughly as much total weight as the top 20, although each tail disease
     * counts for little. A larger pool therefore adds randomness to the sampling, and a smaller one makes it
     * concentrate on the leading diseases.
     */
    static final int N_SAMPLING_DIAGNOSES = 500;

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
        this(maxoToHpoTermIdMap, maxoHpoTermProbabilities, engine, hpo, allInitialDiagnoses,
                initialDiagnoses, hpoFrequenciesNDiseases, ProgressListener.NONE);
    }

    /** @param progressListener told each time the evaluation of one MAxO term has finished */
    public RankMaxo(Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                    MaxoHpoTermProbabilities maxoHpoTermProbabilities,
                    DDxEngine engine,
                    MinimalOntology hpo,
                    List<DifferentialDiagnosis> allInitialDiagnoses,
                    List<DifferentialDiagnosis> initialDiagnoses,
                    List<HpoFrequency> hpoFrequenciesNDiseases,
                    ProgressListener progressListener) {
        this.maxoToHpoTermIdMap = maxoToHpoTermIdMap;
        this.maxoHpoTermProbabilities = maxoHpoTermProbabilities;
        this.engine = engine;
        this.allInitialDiagnoses = allInitialDiagnoses;
        this.initialDiagnoses = initialDiagnoses;
        this.hpoFrequenciesNDiseases = hpoFrequenciesNDiseases;
        this.progressListener = Objects.requireNonNull(progressListener, "progressListener must not be null");
    }

    /**
     *
     * @param ppkt Input phenopacket with present and excluded HPO terms.
     * @param diseaseIds Set of top n OMIM disease Ids to use for analysis.
     * @return Map of MAxO scores sorted in descending order by score
     */
    public List<RankedMaxoResult> rankMaxoTerms(PhenopacketData ppkt,
                                                Set<TermId> diseaseIds,
                                                MdContext context,
                                                ExecutorService sharedExecutorService) throws Exception {

        int nRepetitions = context.params().nRepetitions();
        BiometadataService biometadataService = context.biometadataService();
        List<TermId> ppktMaxoIds = ppkt.maxoProcedureIds();
        AssessablePhenotypes ascertainablePhenotypes = new AssessablePhenotypes(maxoHpoTermProbabilities.getHpoDiseases());
        Map<TermId, Set<TermId>> fullMaxoToHpoTermIdMap = maxoHpoTermProbabilities.getMaxoToHpoTermIdMap();
        AtomicInteger completedTasks = new AtomicInteger(0);
        List<Callable<RankedMaxoResult>> tasks = new ArrayList<>();
        int nTasks = countTasks(maxoToHpoTermIdMap.keySet(), ppktMaxoIds);          // how many terms will really be evaluated
        RankMaxoProgress progressTracker = new RankMaxoProgress(nTasks);            // for the web progress page
        this.rankMaxoProgress = progressTracker;
           for (TermId maxoId : maxoToHpoTermIdMap.keySet()) {
            LOGGER.debug("MAxO id: '{}'", maxoId);
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
                    .nDiagnoses(samplingPoolSize(allInitialDiagnoses.size()))
                    .maxoLabel(biometadataService.maxoLabel(maxoId.getValue()).get())
                    .build();

               tasks.add(withProgress(
                       () -> new MaxoTermEvaluator(maxoHpoDiseaseRank, nRepetitions, ppkt, engine,
                               initialDiagnoses, diseaseIds, biometadataService, hpoFrequenciesNDiseases).call(),
                       completedTasks, nTasks, progressListener, progressTracker, maxoId.getValue()));
        }

        List<Future<RankedMaxoResult>> futures = sharedExecutorService.invokeAll(tasks);
        List<RankedMaxoResult> results = new ArrayList<>(futures.size());   // RankedMaxoResultSingleDisease in the other method
        for (Future<RankedMaxoResult> future : futures) {
            results.add(resultOf(future));
        }

        return results.stream()
                .sorted(Comparator.comparing(RankedMaxoResult :: maxoScore).reversed())
                .toList();
    }

    public List<RankedMaxoResultSingleDisease> getDiseaseBestMaxoTerms(PhenopacketData ppkt,
                                                                       TermId targetDiseaseId,
                                                                       Set<TermId> diseaseIds,
                                                                       MdContext context,
                                                                       ExecutorService sharedExecutorService) throws InterruptedException {

        int nRepetitions = context.params().nRepetitions();
        BiometadataService biometadataService = context.biometadataService();
        List<TermId> ppktMaxoIds = ppkt.maxoProcedureIds();
        Map<TermId, Set<TermId>> fullMaxoToHpoTermIdMap = maxoHpoTermProbabilities.getMaxoToHpoTermIdMap();

        AtomicInteger completedTasks = new AtomicInteger(0);
        List<Callable<RankedMaxoResultSingleDisease>> tasks = new ArrayList<>();
        int nTasks = countTasks(maxoToHpoTermIdMap.keySet(), ppktMaxoIds);
        RankMaxoProgress progressTracker = new RankMaxoProgress(nTasks);
        this.rankMaxoProgress = progressTracker;

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
                    .nDiagnoses(samplingPoolSize(allInitialDiagnoses.size()))
                    .maxoLabel(biometadataService.maxoLabel(maxoId.getValue()).get())
                    .build();

            tasks.add(withProgress(
                    () -> new MaxoTermEvaluatorSingleDisease(maxoHpoDiseaseRank, nRepetitions, ppkt, engine,
                            maxoHpoTermProbabilities, initialDiagnoses, diseaseIds, biometadataService,
                            context, targetDiseaseId).call(),
                    completedTasks, nTasks, progressListener, progressTracker, maxoId.getValue()));
        }

        List<Future<RankedMaxoResultSingleDisease>> futures = sharedExecutorService.invokeAll(tasks);
        List<RankedMaxoResultSingleDisease> results = new ArrayList<>(futures.size());
        for (Future<RankedMaxoResultSingleDisease> future : futures) {
            results.add(resultOf(future));
        }

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

    /** @return the pool size to use, which can't be larger than the number of diagnoses available */
    static int samplingPoolSize(int nAvailable) {
        return Math.min(N_SAMPLING_DIAGNOSES, nAvailable);
    }

    /**
     * {@code invokeAll} has already waited for every task, so this returns at once; a task that failed throws here.
     */
    static <T> T resultOf(Future<T> future) {
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw new MaxodiffRuntimeException("Evaluation of a MAxO term failed: " + e.getCause(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore the flag for whoever owns this thread
            throw new MaxodiffRuntimeException("Interrupted while collecting MAxO results", e);
        }
    }

    /** @return the number of MAxO terms that will be evaluated: those the patient doesn't already have */
    static int countTasks(Collection<TermId> maxoIds, List<TermId> alreadyInPatient) {
        return (int) maxoIds.stream().filter(id -> !alreadyInPatient.contains(id)).count();
    }

    /** Wraps a task so that progress is recorded and reported only after the task has finished successfully. */
    static <T> Callable<T> withProgress(Callable<T> task, AtomicInteger completedTasks, int nTasks,
                                        ProgressListener listener, RankMaxoProgress tracker, String maxoId) {
        return () -> {
            T result = task.call();
            int done = completedTasks.incrementAndGet();
            tracker.updateProgress(maxoId, done);
            listener.onTaskCompleted(done, nTasks);
            return result;
        };
    }

}
