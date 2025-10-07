package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.*;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NewEvaluateMaxoTerm implements Callable<RankMaxoScore> {

    private final MaxoHpoDiseaseRank maxoHpoDiseaseRank;
    private final int nRepetitions;
    private final Sample ppkt;
    private final DifferentialDiagnosisEngine engine;
    private final MaxoHpoTermProbabilities maxoHpoTermProbabilities;
    private final Set<TermId> diseaseIds;

    public NewEvaluateMaxoTerm(MaxoHpoDiseaseRank maxoHpoDiseaseRank,
                               int nRepetitions, Sample ppkt, DifferentialDiagnosisEngine engine,
                               MaxoHpoTermProbabilities maxoHpoTermProbabilities, Set<TermId> diseaseIds) {
        this.maxoHpoDiseaseRank = maxoHpoDiseaseRank;
        this.nRepetitions = nRepetitions;
        this.ppkt = ppkt;
        this.engine = engine;
        this.maxoHpoTermProbabilities = maxoHpoTermProbabilities;
        this.diseaseIds = diseaseIds;
    }

    @Override
    public RankMaxoScore call() {

        List<Integer> ascertainedHpoCtList = maxoHpoDiseaseRank.getAscertainedHpoCountList();
        Map<TermId, Double> hpoToProbabilityMap = maxoHpoDiseaseRank.getHpoToProbabiltyMap();

        List<TermId> hpoIds = new ArrayList<>();
        List<Double> probabilities = new ArrayList<>();

        for (Map.Entry<TermId, Double> entry : hpoToProbabilityMap.entrySet()) {
            TermId hpoId = entry.getKey();
            Double probability = entry.getValue();
            hpoIds.add(hpoId);
            probabilities.add(probability);
        }

        if (hpoIds.size() != probabilities.size()) {
            throw new IllegalArgumentException("hpoIds and probabilities must have the same size.");
        }

        List<DifferentialDiagnosis> initialDiagnoses = maxoHpoTermProbabilities.getInitialDiagnoses();
        List<List<DifferentialDiagnosis>> newMaxoDiagnosesList = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        // Choose average count HPO terms from hpoList according to probability. (Poisson sample)
        Set<TermId> chosenHpoIds = new HashSet<>();
        Map<TermId, Integer> chosenHpoTermCountsMap = new HashMap<>();
        for (int i = 0; i < nRepetitions; i++) {
            double meanOpt = ascertainedHpoCtList.stream().mapToInt(Integer::intValue).average().orElse(1);
            chosenHpoIds.addAll(selectKWeightedHpoTerms(hpoIds, probabilities, (int) meanOpt));
            for (TermId hpoId : chosenHpoIds) {
                if (!chosenHpoTermCountsMap.containsKey(hpoId)) {
                    chosenHpoTermCountsMap.put(hpoId, 1);
                } else {
                    chosenHpoTermCountsMap.put(hpoId, chosenHpoTermCountsMap.get(hpoId) + 1);
                }
            }

            Set<TermId> observed = chosenHpoIds;
            Set<TermId> excluded = new HashSet<>(List.of());
            Sample newSample = getNewSample(ppkt, observed, excluded);
            List<DifferentialDiagnosis> newMaxoDiagnoses = engine.run(newSample, diseaseIds);
            newMaxoDiagnosesList.add(newMaxoDiagnoses);

            double finalScore = ValidationModel.weightedRankDiff(initialDiagnoses, newMaxoDiagnoses).validationScore();
            scores.add(finalScore);
        }
        OptionalDouble meanScoreOptional = scores.stream().mapToDouble(s -> s).average();
        double meanScore = meanScoreOptional.orElse(0.0);
        return makeRankMaxoScore(chosenHpoIds, meanScore, initialDiagnoses, newMaxoDiagnosesList, chosenHpoTermCountsMap);
    }

    public static List<TermId> selectKWeightedHpoTerms(List<TermId> hpoIds, List<Double> probabilities, int k) {
        // Create cumulative probabilities
        List<Double> cumulative = new ArrayList<>();
        double cumSum = 0.0;
        for (double p : probabilities) {
            cumSum += p;
            cumulative.add(cumSum);
        }

        // Perform weighted sampling without replacement
        List<TermId> selected = new ArrayList<>();
        Set<Integer> usedIndices = new HashSet<>();
        Random random = new Random();

        while (selected.size() < k) {
            double r = random.nextDouble();
            for (int i = 0; i < cumulative.size(); i++) {
                if (r <= cumulative.get(i) && !usedIndices.contains(i)) {
                    selected.add(hpoIds.get(i));
                    usedIndices.add(i);
                    break;
                }
            }
        }

        return selected;
    }

    private Sample getNewSample(Sample ppkt, Set<TermId> observed, Set<TermId> excluded) {
        Set<TermId> ppktObserved = new HashSet<>(ppkt.observedHpoTermIds());
        Set<TermId> ppktExcluded = new HashSet<>(ppkt.excludedHpoTermIds());
        Set<TermId> newObserved = Stream.concat(ppktObserved.stream(), observed.stream()).collect(Collectors.toSet());
        Set<TermId> newExcluded = Stream.concat(ppktExcluded.stream(), excluded.stream()).collect(Collectors.toSet());

        return Sample.of(ppkt.id(), newObserved, newExcluded);
    }

    private RankMaxoScore makeRankMaxoScore(Set<TermId> chosenHpoIds, double meanScore,
                                            List<DifferentialDiagnosis> initialDiagnoses,
                                            List<List<DifferentialDiagnosis>> newMaxoDiagnosesList,
                                            Map<TermId, Integer> chosenHpoTermCountsMap) {

        Set<TermId> initialDiagnosesDiseaseIds = initialDiagnoses.stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toSet());
        Set<TermId> maxoDiagnosesDiseaseIds = newMaxoDiagnosesList.getFirst().stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toSet());

        Set<TermId> maxoDiscoverableObservedHpoIds = chosenHpoIds;
        Set<TermId> maxoObservedDescendantHpoIds = Set.of();

        Map<TermId, List<Integer>> maxoDiseaseAvgRankChangeMap = new HashMap<>();
        Map<TermId, Map<TermId, Integer>> maxoDiscoverableHpoIdCts = new HashMap<>();
        for (TermId omimId : maxoDiagnosesDiseaseIds) {
            int initialRank = 0;
            List<DifferentialDiagnosis> initialDiffDiagnoses = initialDiagnoses.stream()
                    .filter(dd -> dd.diseaseId().equals(omimId)).toList();
            if (!initialDiffDiagnoses.isEmpty()) {
                DifferentialDiagnosis initialDiagnosis = initialDiffDiagnoses.getFirst();
                initialRank = initialDiagnoses.indexOf(initialDiagnosis) + 1;
            }

            List<Integer> rankDiffs = new ArrayList<>();
            for (List<DifferentialDiagnosis> newMaxoDiagnoses : newMaxoDiagnosesList) {
                List<DifferentialDiagnosis> maxoDiagnoses = newMaxoDiagnoses.stream()
                        .filter(dd -> dd.diseaseId().equals(omimId)).toList();
                if (!maxoDiagnoses.isEmpty()) {
                    DifferentialDiagnosis maxoDiagnosis = maxoDiagnoses.getFirst();
                    int maxoRank = newMaxoDiagnoses.indexOf(maxoDiagnosis) + 1;
                    int maxoRankDiff = maxoRank - initialRank;
                    rankDiffs.add(maxoRankDiff);
                }
            }

            double meanRankDiffDouble = rankDiffs.stream().mapToDouble(s -> s).average().orElse(0);
            int meanRankDiff = (int) Math.round(meanRankDiffDouble);
            List<Integer> rankChanges = new ArrayList<>();
            rankChanges.add(initialRank);
            rankChanges.add(meanRankDiff);
            maxoDiseaseAvgRankChangeMap.put(omimId, rankChanges);

            List<TermId> diseaseAssociatedHpoIds = List.of();
            Optional<HpoDisease> opt = maxoHpoTermProbabilities.getHpoDiseases().diseaseById(omimId);
            if (opt.isPresent()) {
                HpoDisease disease = opt.get();
                diseaseAssociatedHpoIds = disease.annotationTermIdList();
            }
            Map<TermId, Integer> hpoIdCtsMap = new HashMap<>();
            for (TermId discoverableHpoId : chosenHpoIds) {
                if (diseaseAssociatedHpoIds.contains(discoverableHpoId)) {
                    hpoIdCtsMap.put(discoverableHpoId, chosenHpoTermCountsMap.get(discoverableHpoId));
                }
            }
            maxoDiscoverableHpoIdCts.put(omimId, hpoIdCtsMap);
        }
        //sort maps by disease average rank change
        Map<TermId, List<Integer>> maxoDiseaseAvgRankChangeMapSorted = maxoDiseaseAvgRankChangeMap.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().get(entry.getValue().size() - 1)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a,b)->b, LinkedHashMap::new));

        Map<TermId, Map<TermId, Integer>> maxoDiscoverableHpoIdCtsSorted = maxoDiseaseAvgRankChangeMapSorted.keySet().stream()
                .filter(maxoDiscoverableHpoIdCts::containsKey)
                .collect(Collectors.toMap(
                        key -> key,
                        maxoDiscoverableHpoIdCts::get,
                        (oldValue, newValue) -> newValue,
                        LinkedHashMap::new
                ));

        int minRankChange = maxoDiseaseAvgRankChangeMapSorted.entrySet().stream().toList().getFirst().getValue().getLast();
        int maxRankChange = maxoDiseaseAvgRankChangeMapSorted.entrySet().stream().toList().getLast().getValue().getLast();

        return new RankMaxoScore(maxoHpoDiseaseRank.getMaxoId(), initialDiagnosesDiseaseIds, maxoDiagnosesDiseaseIds,
                maxoDiscoverableObservedHpoIds, chosenHpoTermCountsMap, meanScore,
                newMaxoDiagnosesList.getFirst(),
                maxoDiscoverableHpoIdCtsSorted, maxoDiseaseAvgRankChangeMapSorted,
                minRankChange, maxRankChange);
    }
}
