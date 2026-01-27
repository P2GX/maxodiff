package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.*;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NewEvaluateMaxoTerm implements Callable<RankMaxoScore> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewEvaluateMaxoTerm.class);

    private final MaxoHpoDiseaseRank maxoHpoDiseaseRank;
    private final int nRepetitions;
    private final Sample ppkt;
    private final DifferentialDiagnosisEngine engine;
    private final MaxoHpoTermProbabilities maxoHpoTermProbabilities;
    private final Set<TermId> diseaseIds;
    private final List<DifferentialDiagnosis> initialDiagnoses;

    public NewEvaluateMaxoTerm(
            MaxoHpoDiseaseRank maxoHpoDiseaseRank,
            int nRepetitions,
            Sample ppkt,
            DifferentialDiagnosisEngine engine,
            MaxoHpoTermProbabilities maxoHpoTermProbabilities,
            List<DifferentialDiagnosis> initialDiagnoses,
            Set<TermId> diseaseIds) {
        this.maxoHpoDiseaseRank = maxoHpoDiseaseRank;
        this.nRepetitions = nRepetitions;
        this.ppkt = ppkt;
        this.engine = engine;
        this.maxoHpoTermProbabilities = maxoHpoTermProbabilities;
        this.diseaseIds = diseaseIds;
        this.initialDiagnoses = initialDiagnoses;
    }

    @Override
    public RankMaxoScore call() {

        Map<TermId, Double> hpoToProbabilityMap = maxoHpoDiseaseRank.getHpoToProbabiltyMap();
        List<Integer> nHposToSample = maxoHpoDiseaseRank.getSampledHpoCounts(nRepetitions);

        // Separate HPO IDs and their probabilities
        List<TermId> hpoIds = new ArrayList<>(hpoToProbabilityMap.keySet());
        List<Double> probabilities = new ArrayList<>(hpoToProbabilityMap.values());

        // Run simulations and calculate final scores
        List<List<DifferentialDiagnosis>> newMaxoDiagnosesList = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        Set<TermId> simulatedHpoIdSet = new HashSet<>();
        Map<TermId, Integer> simulatedHpoCountSet = new HashMap<>();
        for (int i = 0; i < nRepetitions; i++) {
            // Sample and count simulated HPO terms
            int nHpos = nHposToSample.get(i);
            simulatedHpoIdSet.addAll(selectKWeightedHpoTerms(hpoIds, probabilities, nHpos));
            simulatedHpoIdSet.forEach(hpoId -> simulatedHpoCountSet.merge(hpoId, 1, Integer::sum));

            Sample newSample = getNewSample(ppkt, simulatedHpoIdSet);
            List<DifferentialDiagnosis> newMaxoDiagnoses = engine.run(newSample, diseaseIds);
            newMaxoDiagnosesList.add(newMaxoDiagnoses);

            double finalScore = ValidationModel.weightedRankDiff(initialDiagnoses, newMaxoDiagnoses).validationScore();
            scores.add(finalScore);
        }
        double meanScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return makeRankMaxoScore(simulatedHpoIdSet, meanScore, initialDiagnoses, newMaxoDiagnosesList, simulatedHpoCountSet);
    }

    /**
     * Randomly selects {@code k} unique HPO term IDs from a weighted probability distribution.
     * <p>
     * Each HPO term in {@code hpoIds} is associated with a probability in {@code probabilities},
     * defining how likely it is to be selected. Sampling is performed <b>without replacement</b>,
     * meaning each term can only be selected once.
     * </p>
     *
     * @param hpoIds        list of HPO term identifiers
     * @param probabilities list of normalized probabilities corresponding to each HPO term
     * @param k              number of unique HPO terms to sample
     * @return a list of {@code k} sampled HPO term IDs
     */
    public static List<TermId> selectKWeightedHpoTerms(List<TermId> hpoIds, List<Double> probabilities, int k) {
        // Create cumulative probabilities
        List<Double> cumulative = new ArrayList<>(probabilities.size());
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

    /** Create a new sample by adding the simulated observed terms (from the MAxO diagnostic test) to
     * the existing phenopacket and keeping the original excluded terms
     * @param ppkt A Sample object representing the original phenopacket
     * @param observed A Set of simulated new observed HPO terms
     * @return The modified (simulated) phenopacket Sample.
     */
    private Sample getNewSample(Sample ppkt, Set<TermId> observed) {
        Set<TermId> ppktObserved = new HashSet<>(ppkt.observedHpoTermIds());
        Set<TermId> combinedObserved = Stream.concat(ppktObserved.stream(), observed.stream()).collect(Collectors.toSet());

        return Sample.of(ppkt.id(), combinedObserved, ppkt.excludedHpoTermIds());
    }


    /**
     * Returns the 1-based rank (position) of a disease within a list of differential diagnoses.
     * <p>
     * The method iterates through the provided list of {@link DifferentialDiagnosis} objects
     * (representing the ordered initial ranks assigned by Phenomizer)
     * and compares each diagnosis's disease ID to the given {@link TermId}. If a match is found,
     * the method returns the index position (starting from 1). If the disease is not present
     * in the list, the method returns {@code 0}.
     * </p>
     *
     * @param diagnoses the ordered list of {@link DifferentialDiagnosis} instances to search
     * @param diseaseId the {@link TermId} of the disease whose rank should be determined
     * @return the 1-based rank of the disease in the list, or {@code 0} if not found
     */
    private int findRank(List<DifferentialDiagnosis> diagnoses, TermId diseaseId) {
        for (int i = 0; i < diagnoses.size(); i++) {
            if (diagnoses.get(i).diseaseId().equals(diseaseId)) {
                return i + 1; // 1-based rank
            }
        }
        return 0;
    }

    /** Compute average changes in disease ranks between the initial disease rankings and the MAxO disease rankings.
     *
     * @param initialDiagnoses List of diseases from the original differential diagnosis.
     * @param newMaxoDiagnosesList List of diseases from the differential diagnosis using the new sample with additional
     *                             HPO terms that can be ascertained by the MAxO term.
     * @param diseaseIds Set of disease ids for the top n diseases.
     *
     * @return Map of disease OMIM id : List of [initial rank, rank change]
     */
    private Map<TermId, List<Integer>> computeDiseaseAverageRankChanges(
            List<DifferentialDiagnosis> initialDiagnoses,
            List<List<DifferentialDiagnosis>> newMaxoDiagnosesList,
            Set<TermId> diseaseIds) {

        Map<TermId, List<Integer>> avgRankChange = new HashMap<>();

        for (TermId omimId : diseaseIds) {
            int initialRank = findRank(initialDiagnoses, omimId);

            List<Integer> rankDiffs = new ArrayList<>();
            for (List<DifferentialDiagnosis> newList : newMaxoDiagnosesList) {
                int newRank = findRank(newList, omimId);
                if (newRank > 0) {
                    rankDiffs.add(newRank - initialRank);
                }
            }

            double avg = rankDiffs.stream().mapToDouble(Double::valueOf).average().orElse(0);
            avgRankChange.put(omimId, List.of(initialRank, (int) Math.round(avg)));
        }

        return avgRankChange;
    }

    private Set<TermId> extractDiseaseIds(List<DifferentialDiagnosis> diagnoses) {
        return diagnoses.stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Compute average changes in disease ranks between the initial disease rankings and the MAxO disease rankings.
     *
     * @param chosenHpoIds The set of HPO term IDs that were selected (ascertained) in the simulations
     * @param chosenHpoTermCountsMap A mapping of HPO term IDs to how many times each term was selected across simulations
     * @param diseaseIds Set of disease ids for the top n diseases.
     *
     * @return Map of disease id : Map of hpo id : count of hpo id occurrences across simulations.
     */
    private Map<TermId, Map<TermId, Integer>> computeDiscoverableHpoCounts(
            Set<TermId> chosenHpoIds,
            Map<TermId, Integer> chosenHpoTermCountsMap,
            Set<TermId> diseaseIds) {

        Map<TermId, Map<TermId, Integer>> result = new HashMap<>();

        for (TermId omimId : diseaseIds) {
            Optional<HpoDisease> opt = maxoHpoTermProbabilities.getHpoDiseases().diseaseById(omimId);
            if (opt.isEmpty()) continue;

            List<TermId> annotatedHpoIds = opt.get().annotationTermIdList();
            Map<TermId, Integer> hpoCounts = new HashMap<>();

            for (TermId hpoId : chosenHpoIds) {
                if (annotatedHpoIds.contains(hpoId)) {
                    hpoCounts.put(hpoId, chosenHpoTermCountsMap.get(hpoId));
                }
            }

            result.put(omimId, hpoCounts);
        }

        return result;
    }

    private Map<TermId, List<Integer>> sortByRankChange(Map<TermId, List<Integer>> map) {
        return map.entrySet().stream()
                .sorted(Comparator.comparing(entry ->
                        entry.getValue()
                                .get(entry.getValue().size() - 1))) //rank change is last integer in list
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> b,
                        LinkedHashMap::new
                ));
    }

    private Map<TermId, Map<TermId, Integer>> sortHpoCountMapByRankChange(
            Map<TermId, List<Integer>> rankChangeMap,
            Map<TermId, Map<TermId, Integer>> hpoCountMap) {

        return rankChangeMap.keySet().stream()
                .filter(hpoCountMap::containsKey)
                .collect(Collectors.toMap(
                        key -> key,
                        hpoCountMap::get,
                        (a, b) -> b,
                        LinkedHashMap::new
                ));
    }

    /**
     * Constructs a {@link RankMaxoScore} summarizing how diseases and HPO terms change
     * in diagnostic ranking after simulated ascertainment of additional phenotypes.
     * <p>
     * This method aggregates rank changes across repeated simulation runs, associates
     * each disease with the corresponding ascertainable HPO terms, and computes
     * summary statistics such as mean rank change and overall validation score.
     * The output {@link RankMaxoScore} captures both disease-level and phenotype-level
     * effects of adding MAxO-derived HPO terms to the analysis.
     * </p>
     *
     * <h3>Processing steps:</h3>
     * <ol>
     *   <li>Compute the disease IDs from the initial and MAxO-adjusted differential diagnosis lists.</li>
     *   <li>For each disease:
     *     <ul>
     *       <li>Determine its initial rank and rank in each simulation.</li>
     *       <li>Calculate the mean rank change across all simulation runs.</li>
     *       <li>Collect ascertainable HPO terms associated with that disease and record how often they were sampled.</li>
     *     </ul>
     *   </li>
     *   <li>Sort diseases by their average rank change.</li>
     *   <li>Build a {@link RankMaxoScore} containing the sorted disease metrics, associated HPO term counts,
     *       and summary statistics (minimum and maximum rank change values, mean score, etc.).</li>
     * </ol>
     *
     * @param chosenHpoIds              the set of HPO term IDs that were selected (ascertained) in this simulation
     * @param meanScore                 the average validation score across all repetitions (typically derived from rank differences)
     * @param initialDiagnoses          the original list of {@link DifferentialDiagnosis} instances before MAxO-based adjustment
     * @param newMaxoDiagnosesList      a list of differential diagnosis lists, one for each simulation repetition
     * @param chosenHpoTermCountsMap    a mapping of HPO term IDs to how many times each term was selected across simulations
     * @return a {@link RankMaxoScore} summarizing disease rank shifts, ascertainable HPO associations, and aggregate scoring
     */
    private RankMaxoScore makeRankMaxoScore(
            Set<TermId> chosenHpoIds,
            double meanScore,
            List<DifferentialDiagnosis> initialDiagnoses,
            List<List<DifferentialDiagnosis>> newMaxoDiagnosesList,
            Map<TermId, Integer> chosenHpoTermCountsMap) {

        // Step 1: extract ID sets
        Set<TermId> initialIds = extractDiseaseIds(initialDiagnoses);
        Set<TermId> maxoIds = extractDiseaseIds(newMaxoDiagnosesList.getFirst());

        // Step 2: compute average rank changes per disease
        Map<TermId, List<Integer>> avgRankChange = computeDiseaseAverageRankChanges(
                initialDiagnoses, newMaxoDiagnosesList, maxoIds
        );

        // Step 3: compute discoverable HPO term counts per disease
        Map<TermId, Map<TermId, Integer>> hpoCountMap = computeDiscoverableHpoCounts(
                chosenHpoIds, chosenHpoTermCountsMap, maxoIds
        );

        // Step 4: sort both maps by average rank change
        Map<TermId, List<Integer>> avgRankChangeSorted = sortByRankChange(avgRankChange);
        Map<TermId, Map<TermId, Integer>> hpoCountMapSorted = sortHpoCountMapByRankChange(avgRankChangeSorted, hpoCountMap);

        int minRankChange = avgRankChangeSorted.entrySet().stream().toList().getFirst().getValue().getLast();
        int maxRankChange = avgRankChangeSorted.entrySet().stream().toList().getLast().getValue().getLast();

        // Step 5: construct final score
        return new RankMaxoScore(
                maxoHpoDiseaseRank.getMaxoId(),
                initialIds,
                maxoIds,
                chosenHpoIds,
                chosenHpoTermCountsMap,
                meanScore,
                newMaxoDiagnosesList.getFirst(),
                hpoCountMapSorted,
                avgRankChangeSorted,
                minRankChange,
                maxRankChange
        );
    }
}
