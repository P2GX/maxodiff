package org.p2gx.maxodiff.core.analysis;

import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.p2gx.maxodiff.core.model.*;
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
    private final PhenopacketData ppkt;
    private final DDxEngine engine;
    private final MaxoHpoTermProbabilities maxoHpoTermProbabilities;
    private final Set<TermId> diseaseIds;
    private final List<DifferentialDiagnosis> initialDiagnoses;
    private final BiometadataService biometadataService;
    private final List<HpoFrequency> hpoFrequenciesNDiseases;

    public MaxoTermEvaluator(
            MaxoHpoDiseaseRank maxoHpoDiseaseRank,
            int nRepetitions,
            PhenopacketData ppkt,
            DDxEngine engine,
            MaxoHpoTermProbabilities maxoHpoTermProbabilities,
            List<DifferentialDiagnosis> initialDiagnoses,
            Set<TermId> diseaseIds, BiometadataService biometadataService,
            List<HpoFrequency> hpoFrequenciesNDiseases) {
        this.maxoHpoDiseaseRank = maxoHpoDiseaseRank;
        this.nRepetitions = nRepetitions;
        this.ppkt = ppkt;
        this.engine = engine;
        this.maxoHpoTermProbabilities = maxoHpoTermProbabilities;
        this.diseaseIds = diseaseIds;
        this.initialDiagnoses = initialDiagnoses;
        this.biometadataService = biometadataService;
        this.hpoFrequenciesNDiseases = hpoFrequenciesNDiseases;
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
        Set<MySimpleTerm> simulatedHpoIdSet = new HashSet<>();
        Map<TermId, Integer> simulatedHpoCountSet = new HashMap<>();
        for (int i = 0; i < nRepetitions; i++) {
            LOGGER.info("Running repetition " + i + " of " + nRepetitions);
            // Sample and count simulated HPO terms
            int nHpos = nHposToSample.get(i);
            simulatedHpoIdSet.addAll(selectKWeightedHpoTerms(hpoIds, probabilities, nHpos, biometadataService));
//            simulatedHpoIdSet = new HashSet<>(selectKWeightedHpoTerms(hpoIds, probabilities, nHpos));
            simulatedHpoIdSet.forEach(hpoTerm -> simulatedHpoCountSet.merge(hpoTerm.tid(), 1, Integer::sum));

            PhenopacketData newSample = getNewSample(ppkt, simulatedHpoIdSet);
            List<DifferentialDiagnosis> newMaxoDiagnoses = engine.run(newSample, diseaseIds);
            newMaxoDiagnosesList.add(newMaxoDiagnoses);

            double finalScore = ValidationModel.weightedRankDiff(initialDiagnoses, newMaxoDiagnoses).validationScore();
            scores.add(finalScore);
        }
        double meanScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return makeRankedMaxoResult(simulatedHpoIdSet, meanScore, initialDiagnoses, newMaxoDiagnosesList,
                simulatedHpoCountSet, hpoFrequenciesNDiseases);
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
    public static List<MySimpleTerm> selectKWeightedHpoTerms(List<TermId> hpoIds, List<Double> probabilities, int k, BiometadataService biometadataService) {
        // Create cumulative probabilities
        List<Double> cumulative = new ArrayList<>(probabilities.size());
        double cumSum = 0.0;
        for (double p : probabilities) {
            cumSum += p;
            cumulative.add(cumSum);
        }

        // Perform weighted sampling without replacement
        List<MySimpleTerm> selected = new ArrayList<>();
        Set<Integer> usedIndices = new HashSet<>();
        Random random = new Random();

        while (selected.size() < k) {
            double r = random.nextDouble();
            for (int i = 0; i < cumulative.size(); i++) {
                if (r <= cumulative.get(i) && !usedIndices.contains(i)) {
                    TermId hpoId = hpoIds.get(i);
                    selected.add(new MySimpleTerm(hpoId, biometadataService.hpoLabel(hpoId).orElse("unknown")));
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
    private PhenopacketData getNewSample(PhenopacketData ppkt, Set<MySimpleTerm> observed) {
        List<MySimpleTerm> newObservedHpos = Stream.concat(
                        ppkt.observed().stream(),
                        observed.stream().map(st -> new MySimpleTerm(st.tid(), st.label()))
                )
                .distinct() // Ensures uniqueness
                .toList();
        return new PhenopacketData(ppkt.sampleId(), newObservedHpos, ppkt.excluded(), ppkt.diseaseIds(), ppkt.maxoProcedureIds(),false);
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
     * @return List of RankedOmimTerm objects
     */
    private List<RankedOmimTerm> getRankedOmimTermList(
            List<DifferentialDiagnosis> initialDiagnoses,
            List<List<DifferentialDiagnosis>> newMaxoDiagnosesList,
            Set<TermId> diseaseIds) {

        List<RankedOmimTerm> rankedOmimTermList = new ArrayList<>();

        for (TermId diseaseId : diseaseIds) {
            String diseaseLabel = biometadataService.diseaseLabel(diseaseId).orElse("unknown");
            MySimpleTerm omimTerm = new MySimpleTerm(diseaseId, diseaseLabel);
            int initialRank = findRank(initialDiagnoses, diseaseId);

            List<Integer> newRanks = new ArrayList<>();
            for (List<DifferentialDiagnosis> newList : newMaxoDiagnosesList) {
                int newRank = findRank(newList, diseaseId);
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
            Set<MySimpleTerm> chosenHpoIds,
            Map<TermId, Integer> chosenHpoTermCountsMap) {

        List<CountedHpoTerm> result = new ArrayList<>();

        for (MySimpleTerm hpoTerm : chosenHpoIds) {
            TermId hpoId = hpoTerm.tid();
            CountedHpoTerm countedHpoTerm = new CountedHpoTerm(hpoTerm, chosenHpoTermCountsMap.get(hpoId));
            if (!result.contains(countedHpoTerm)) {
                result.add(countedHpoTerm);
            }
        }

        return result;
    }


    /**
     *
     * @param omimIds disease Ids.
     * @param hpoIds hpo Ids.
     * @param hpoTermFrequencies Map of HPO Term Id and List of HpoFrequency objects.
     * @return List of Frequencies records
     */
    private List<HpoFrequency> getFrequencyRecords(Set<TermId> omimIds, Set<MySimpleTerm> hpoIds,
                                                  List<HpoFrequency> hpoTermFrequencies) {

        List<HpoFrequency> frequencyRecords = new ArrayList<>();
        //Set<TermId> omimIds = maxoTermScoreRecord.omimTermIds();
        for (MySimpleTerm hpoTerm : hpoIds) { //maxoTermScoreRecord.hpoTermIds()
            TermId hpoId = hpoTerm.tid();
            List<HpoFrequency> frequencies = hpoTermFrequencies.stream().filter(f->f.hpoId().equals(hpoId)).toList();
//            if (frequencies != null) {
                for (HpoFrequency hpoFrequency : frequencies) {
                    for (TermId omimId : omimIds) {
                        if (hpoFrequency.diseaseId().equals(omimId)) {
                            frequencyRecords.add(hpoFrequency);
                        }
                    }
                }
//            }
        }
        return frequencyRecords;
    }

    private RankedMaxoResult makeRankedMaxoResult(
            Set<MySimpleTerm> chosenHpoIds,
            double meanScore,
            List<DifferentialDiagnosis> initialDiagnoses,
            List<List<DifferentialDiagnosis>> newMaxoDiagnosesList,
            Map<TermId, Integer> chosenHpoTermCountsMap,
            List<HpoFrequency> hpoFrequenciesNDiseases) {

        LOGGER.info("Making final results list");
        // Step 1: Make MAXO SimpleTerm
        TermId maxoId = maxoHpoDiseaseRank.getMaxoId();
        String maxoLabel = maxoHpoDiseaseRank.getMaxoLabel();
        MySimpleTerm maxoTerm = new MySimpleTerm(maxoId, maxoLabel);

        // Step 2: get OMIMs from the initial Phenomizer plus simulation with ranks (List<RankedOmimTerm>)
        Set<TermId> maxoIds = extractDiseaseIds(newMaxoDiagnosesList.getFirst());
        List<RankedOmimTerm> rankedOmimTermList = getRankedOmimTermList(initialDiagnoses, newMaxoDiagnosesList, maxoIds);

        // Step 3: get set of observed HPO terms discoverable by the MAxO term (List<CountedHpoTerm>)
        List<CountedHpoTerm> countedHpoTerms = getCountedHpoTerms(
                chosenHpoIds, chosenHpoTermCountsMap
        );

        // Step 4: get collection of HPO Term Frequencies (List<Frequencies>)
        List<HpoFrequency> frequencies = getFrequencyRecords(maxoIds, chosenHpoIds, hpoFrequenciesNDiseases);

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
