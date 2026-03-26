package org.p2gx.maxodiff.core.analysis;

import org.p2gx.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;
import org.p2gx.maxodiff.core.model.MaxoHpoDiseaseRank;
import org.p2gx.maxodiff.core.model.MaxoHpoTermProbabilities;
import org.p2gx.maxodiff.core.model.PpktSample;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MaxoTermEvaluator implements Callable<RankedMaxoResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaxoTermEvaluator.class);

    private final MaxoHpoDiseaseRank maxoHpoDiseaseRank;
    private final int nRepetitions;
    private final PpktSample ppkt;
    private final DifferentialDiagnosisEngine engine;
    private final MaxoHpoTermProbabilities maxoHpoTermProbabilities;
    private final Set<TermId> diseaseIds;
    private final List<DifferentialDiagnosis> initialDiagnoses;
    private final BiometadataService biometadataService;

    public MaxoTermEvaluator(
            MaxoHpoDiseaseRank maxoHpoDiseaseRank,
            int nRepetitions,
            PpktSample ppkt,
            DifferentialDiagnosisEngine engine,
            MaxoHpoTermProbabilities maxoHpoTermProbabilities,
            List<DifferentialDiagnosis> initialDiagnoses,
            Set<TermId> diseaseIds, BiometadataService biometadataService) {
        this.maxoHpoDiseaseRank = maxoHpoDiseaseRank;
        this.nRepetitions = nRepetitions;
        this.ppkt = ppkt;
        this.engine = engine;
        this.maxoHpoTermProbabilities = maxoHpoTermProbabilities;
        this.diseaseIds = diseaseIds;
        this.initialDiagnoses = initialDiagnoses;
        this.biometadataService = biometadataService;
    }

    @Override
    public RankedMaxoResult call() {

        Map<TermId, Double> hpoToProbabilityMap = maxoHpoDiseaseRank.getHpoToProbabiltyMap();
        List<Integer> nHposToSample = maxoHpoDiseaseRank.getSampledHpoCounts(nRepetitions);

        // Separate HPO IDs and their probabilities
        List<TermId> hpoIds = new ArrayList<>(hpoToProbabilityMap.keySet());
        List<Double> probabilities = new ArrayList<>(hpoToProbabilityMap.values());

        // Run simulations and calculate final scores
        List<List<DifferentialDiagnosis>> newMaxoDiagnosesList = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        Set<SimpleTerm> simulatedHpoIdSet = new HashSet<>();
        Map<TermId, Integer> simulatedHpoCountSet = new HashMap<>();
        for (int i = 0; i < nRepetitions; i++) {
            // Sample and count simulated HPO terms
            int nHpos = nHposToSample.get(i);
            simulatedHpoIdSet.addAll(selectKWeightedHpoTerms(hpoIds, probabilities, nHpos, biometadataService));
//            simulatedHpoIdSet = new HashSet<>(selectKWeightedHpoTerms(hpoIds, probabilities, nHpos));
            simulatedHpoIdSet.forEach(hpoTerm -> simulatedHpoCountSet.merge(TermId.of(hpoTerm.termId()), 1, Integer::sum));

            PpktSample newSample = getNewSample(ppkt, simulatedHpoIdSet);
            List<DifferentialDiagnosis> newMaxoDiagnoses = engine.run(newSample, diseaseIds);
            newMaxoDiagnosesList.add(newMaxoDiagnoses);

            double finalScore = ValidationModel.weightedRankDiff(initialDiagnoses, newMaxoDiagnoses).validationScore();
            scores.add(finalScore);
        }
        double meanScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return makeRankedMaxoResult(simulatedHpoIdSet, meanScore, initialDiagnoses, newMaxoDiagnosesList, simulatedHpoCountSet);
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
    public static List<SimpleTerm> selectKWeightedHpoTerms(List<TermId> hpoIds, List<Double> probabilities, int k, BiometadataService biometadataService) {
        // Create cumulative probabilities
        List<Double> cumulative = new ArrayList<>(probabilities.size());
        double cumSum = 0.0;
        for (double p : probabilities) {
            cumSum += p;
            cumulative.add(cumSum);
        }

        // Perform weighted sampling without replacement
        List<SimpleTerm> selected = new ArrayList<>();
        Set<Integer> usedIndices = new HashSet<>();
        Random random = new Random();

        while (selected.size() < k) {
            double r = random.nextDouble();
            for (int i = 0; i < cumulative.size(); i++) {
                if (r <= cumulative.get(i) && !usedIndices.contains(i)) {
                    TermId hpoId = hpoIds.get(i);
                    selected.add(new SimpleTerm(hpoId.getValue(), biometadataService.hpoLabel(hpoId).orElse("unknown")));
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
    private PpktSample getNewSample(PpktSample ppkt, Set<SimpleTerm> observed) {
        Set<SimpleTerm> ppktObserved = new HashSet<>(ppkt.observedHpoTerms());
        Set<SimpleTerm> combinedObserved = Stream.concat(ppktObserved.stream(), observed.stream()).collect(Collectors.toSet());

        return new PpktSample(ppkt.id(), combinedObserved.stream().toList(), ppkt.excludedHpoTerms());
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
    private List<RankedOmimTerm> getRankedOmimTermList(
            List<DifferentialDiagnosis> initialDiagnoses,
            List<List<DifferentialDiagnosis>> newMaxoDiagnosesList,
            Set<TermId> diseaseIds) {

        List<RankedOmimTerm> rankedOmimTermList = new ArrayList<>();

        for (TermId omimId : diseaseIds) {
            String omimIdStr = omimId.getValue();
            String omimLabel = biometadataService.diseaseLabel(omimId).orElse("unknown");
            SimpleTerm omimTerm = new SimpleTerm(omimIdStr, omimLabel);
            int initialRank = findRank(initialDiagnoses, omimId);

            List<Integer> newRanks = new ArrayList<>();
            for (List<DifferentialDiagnosis> newList : newMaxoDiagnosesList) {
                int newRank = findRank(newList, omimId);
                if (newRank > 0) {
                    newRanks.add(newRank);
                }
            }

            int avg = (int) newRanks.stream().mapToDouble(Double::valueOf).average().orElse(0);

            RankedOmimTerm rankedOmimTerm = new RankedOmimTerm(omimTerm, initialRank, avg);
            rankedOmimTermList.add(rankedOmimTerm);
        }

        return rankedOmimTermList;
    }

    private Set<TermId> extractDiseaseIds(List<DifferentialDiagnosis> diagnoses) {
        return diagnoses.stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     *
     * @param chosenHpoTermCountsMap A mapping of HPO term IDs to how many times each term was selected across simulations
     *
     * @return List of CountedHpoTerm objects
     */
    private List<CountedHpoTerm> getCountedHpoTerms (
            Set<SimpleTerm> chosenHpoIds,
            Map<TermId, Integer> chosenHpoTermCountsMap) {

        List<CountedHpoTerm> result = new ArrayList<>();

        for (SimpleTerm hpoTerm : chosenHpoIds) {
            TermId hpoId = TermId.of(hpoTerm.termId());
            CountedHpoTerm countedHpoTerm = new CountedHpoTerm(hpoTerm, chosenHpoTermCountsMap.get(hpoId));
            if (!result.contains(countedHpoTerm)) {
                result.add(countedHpoTerm);
            }
        }

        return result;
    }

    /**
     *
     * @param diseases List of Hpo diseases
     * @return Map of HPO Term Id and List of HpoFrequency objects.
     */
    private Map<TermId, List<HpoFrequency>> getHpoTermFrequencies(
            List<HpoDisease> diseases) {
        // Collect HPO terms and frequencies for the target m diseases
        DiseaseTermCount diseaseTermCount = DiseaseTermCount.of(diseases);
        return diseaseTermCount.hpoTermCounts();
    }

    /**
     *
     * @param omimIds disease Ids.
     * @param hpoIds hpo Ids.
     * @param hpoTermFrequencies Map of HPO Term Id and List of HpoFrequency objects.
     * @return List of Frequencies records
     */
    private List<HpoFrequency> getFrequencyRecords(Set<TermId> omimIds, Set<SimpleTerm> hpoIds,
                                                  Map<TermId, List<HpoFrequency>> hpoTermFrequencies) {

        List<HpoFrequency> frequencyRecords = new ArrayList<>();
        //Set<TermId> omimIds = maxoTermScoreRecord.omimTermIds();
        for (SimpleTerm hpoTerm : hpoIds) { //maxoTermScoreRecord.hpoTermIds()
            TermId hpoId = TermId.of(hpoTerm.termId());
            List<HpoFrequency> frequencies = hpoTermFrequencies.get(hpoId);
            if (frequencies != null) {
                for (HpoFrequency hpoFrequency : frequencies) {
                    for (TermId omimId : omimIds) {
                        if (hpoFrequency.omimId().equals(omimId.toString())) {
                            frequencyRecords.add(hpoFrequency);
                        }
                    }
                }
            }
        }
        return frequencyRecords;
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



    private RankedMaxoResult makeRankedMaxoResult(
            Set<SimpleTerm> chosenHpoIds,
            double meanScore,
            List<DifferentialDiagnosis> initialDiagnoses,
            List<List<DifferentialDiagnosis>> newMaxoDiagnosesList,
            Map<TermId, Integer> chosenHpoTermCountsMap) {

        // Step 1: Make MAXO SimpleTerm
        String maxoId = maxoHpoDiseaseRank.getMaxoId().toString();
        String maxoLabel = maxoHpoDiseaseRank.getMaxoLabel();
        SimpleTerm maxoTerm = new SimpleTerm(maxoId, maxoLabel);

        // Step 2: get OMIMs from the initial Phenomizer plus simulation with ranks (List<RankedOmimTerm>)
        Set<TermId> maxoIds = extractDiseaseIds(newMaxoDiagnosesList.getFirst());
        List<RankedOmimTerm> rankedOmimTermList = getRankedOmimTermList(initialDiagnoses, newMaxoDiagnosesList, maxoIds);

        // Step 3: get set of observed HPO terms discoverable by the MAxO term (List<CountedHpoTerm>)
        List<CountedHpoTerm> countedHpoTerms = getCountedHpoTerms(
                chosenHpoIds, chosenHpoTermCountsMap
        );

        // Step 4: get collection of HPO Term Frequencies (List<Frequencies>)
        List<HpoDisease> hpoDiseases = maxoHpoTermProbabilities.getHpoDiseases().hpoDiseases().toList();
        Map<TermId, List<HpoFrequency>> hpoFrequencyMap = getHpoTermFrequencies(hpoDiseases);
        List<HpoFrequency> frequencies = getFrequencyRecords(maxoIds, chosenHpoIds, hpoFrequencyMap);

        // Step 5: construct final result
        return new RankedMaxoResult(
                maxoTerm,
                meanScore,
                rankedOmimTermList,
                countedHpoTerms,
                frequencies
        );
    }
}
