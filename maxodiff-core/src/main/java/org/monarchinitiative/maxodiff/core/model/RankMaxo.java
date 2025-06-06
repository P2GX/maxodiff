package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.EvaluateMaxoTerm;
import org.monarchinitiative.maxodiff.core.analysis.MaxoDDResults;
import org.monarchinitiative.maxodiff.core.analysis.RankMaxoScore;
import org.monarchinitiative.maxodiff.core.analysis.ValidationModel;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class RankMaxo {

    private final Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap;
    private final Map<TermId, Set<TermId>> maxoToHpoTermIdMap;
    private final MaxoHpoTermProbabilities maxoHpoTermProbabilities;
    private final DifferentialDiagnosisEngine engine;
    double progress;
    private final MinimalOntology minimalOntology;
    private final Ontology ontology;

    public RankMaxo(Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap,
                    Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                    MaxoHpoTermProbabilities maxoHpoTermProbabilities,
                    DifferentialDiagnosisEngine engine,
                    MinimalOntology minHpo,
                    Ontology hpo) {
        this.hpoToMaxoTermMap = hpoToMaxoTermMap;
        this.maxoToHpoTermIdMap = maxoToHpoTermIdMap;
        this.maxoHpoTermProbabilities = maxoHpoTermProbabilities;
        this.engine = engine;
        this.minimalOntology = minHpo;
        this.ontology = hpo;
    }

    /**
     *
     * @param ppkt Input phenopacket with present and excluded HPO terms.
     * @param nRepetitions number of times to calculate scores for each MAxO term.
     * @param diseaseIds Set of OMIM disease Ids to use for analysis.
     * @return Map of MAxO scores sorted in descending order by score
     */
    public List<RankMaxoScore> rankMaxoTerms(Sample ppkt, int nRepetitions, Set<TermId> diseaseIds) throws Exception {

        Set<TermId> sampleHpoIds = new HashSet<>();
        sampleHpoIds.addAll(ppkt.presentHpoTermIds());
        sampleHpoIds.addAll(ppkt.excludedHpoTermIds());

        int numThreads = Runtime.getRuntime().availableProcessors() - 1;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicInteger completedTasks = new AtomicInteger(0);
        List<Callable<RankMaxoScore>> tasks = new ArrayList<>();
        for (TermId maxoId : maxoToHpoTermIdMap.keySet()) {
            tasks.add(() -> {
                try {
                    EvaluateMaxoTerm evaluateMaxoTerm = new EvaluateMaxoTerm(hpoToMaxoTermMap, maxoToHpoTermIdMap, maxoHpoTermProbabilities,
                            engine,  minimalOntology, ontology,  sampleHpoIds,  ppkt, nRepetitions, diseaseIds, maxoId);
                    int done = completedTasks.incrementAndGet();
                    System.out.println(done);
                    return evaluateMaxoTerm.call();
                } finally {
//                    rankMaxoService.taskCompleted(); // update progress
                }
            });
        }

        List<Future<RankMaxoScore>> futures = executor.invokeAll(tasks);

        List<RankMaxoScore> results = new ArrayList<>();
        for (Future<RankMaxoScore> future : futures) {
            try {
                results.add(future.get()); // blocks until the result is available
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace(); // handle exceptions properly in production
            }
        }
        executor.shutdown();

        return results.stream()
                .sorted(Comparator.comparing(RankMaxoScore :: maxoScore).reversed())
                .toList();
    }

    private static double calculateRelDiseaseDiffEntropySum(List<Double> differentialDiagnosisScores) {
        double sum = 0.0;
        final double EPSILON = 1e-10;
        for(Double score : differentialDiagnosisScores) {
            //TODO: double-check log base. Default is base e.
            sum += Math.abs(score) < EPSILON ? 0 : Math.log(score)*score;
        }
        return -sum;
    }

    private double updateProgress(double p, int nMaxoTermIds) {
        return (p / nMaxoTermIds) * 100.;
    }

    public double getProgress() {
        return progress;
    }

}
