package org.p2gx.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.model.*;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

public class MaxoTermEvaluatorSingleDisease implements Callable<RankedMaxoResultSingleDisease> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaxoTermEvaluatorSingleDisease.class);

    private final MaxoHpoDiseaseRank maxoHpoDiseaseRank;
    private final int nRepetitions;
    private final PhenopacketData ppkt;
    private final DDxEngine engine;
    private final MaxoHpoTermProbabilities maxoHpoTermProbabilities;
    private final Set<TermId> diseaseIds;
    private final List<DifferentialDiagnosis> initialDiagnoses;
    private final BiometadataService biometadataService;
    private final MdContext context;
    private final TermId targetDiseaseId;

    public MaxoTermEvaluatorSingleDisease(
            MaxoHpoDiseaseRank maxoHpoDiseaseRank,
            int nRepetitions,
            PhenopacketData ppkt,
            DDxEngine engine,
            MaxoHpoTermProbabilities maxoHpoTermProbabilities,
            List<DifferentialDiagnosis> initialDiagnoses,
            Set<TermId> diseaseIds, BiometadataService biometadataService,
            MdContext context,
            TermId targetDiseaseId) {
        this.maxoHpoDiseaseRank = maxoHpoDiseaseRank;
        this.nRepetitions = nRepetitions;
        this.ppkt = ppkt;
        this.engine = engine;
        this.maxoHpoTermProbabilities = maxoHpoTermProbabilities;
        this.diseaseIds = diseaseIds;
        this.initialDiagnoses = initialDiagnoses;
        this.biometadataService = biometadataService;
        this.context = context;
        this.targetDiseaseId = targetDiseaseId;
    }

    public RankedMaxoResultSingleDisease call() {

        Map<TermId, Double> hpoToProbabilityMap = maxoHpoDiseaseRank.getHpoToProbabiltyMap();
        List<Integer> nHposToSample = maxoHpoDiseaseRank.getSampledHpoCounts(nRepetitions);
        DiscoverablePhenotypes discoverablePhenotypes = maxoHpoTermProbabilities.getDiscoverablePhenotypes();

        // Separate HPO IDs and their probabilities
        List<TermId> hpoIds = new ArrayList<>(hpoToProbabilityMap.keySet());
        List<Double> probabilities = new ArrayList<>(hpoToProbabilityMap.values());

        // Run simulations and calculate final scores
        List<List<DifferentialDiagnosis>> newMaxoDiagnosesList = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        Set<MySimpleTerm> simulatedHpoIdSet = new HashSet<>();
        Map<TermId, Integer> simulatedHpoCountSet = new HashMap<>();
        for (int i = 0; i < nRepetitions; i++) {
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
        return makeRankedMaxoResultSingleDisease(targetDiseaseId, meanScore,
                initialDiagnoses, newMaxoDiagnosesList, discoverablePhenotypes, context);
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
     * @param targetDiseaseId disease id for the disease of interest.
     *
     * @return RankedOmimTerm for target disease
     */
    private RankedOmimTerm getRankedOmimTerm(
            List<DifferentialDiagnosis> initialDiagnoses,
            List<List<DifferentialDiagnosis>> newMaxoDiagnosesList,
            TermId targetDiseaseId) {


        String diseaseLabel = biometadataService.diseaseLabel(targetDiseaseId).orElse("unknown");
        MySimpleTerm diseaseTerm = new MySimpleTerm(targetDiseaseId, diseaseLabel);
        int initialRank = findRank(initialDiagnoses, targetDiseaseId);

        List<Integer> newRanks = new ArrayList<>();
        for (List<DifferentialDiagnosis> newList : newMaxoDiagnosesList) {
            int newRank = findRank(newList, targetDiseaseId);
            if (newRank > 0) {
                newRanks.add(newRank);
            }
        }

        int avg = (int) newRanks.stream().mapToDouble(Double::valueOf).average().orElse(0);

        return new RankedOmimTerm(diseaseTerm, initialRank, avg);
    }


    private RankedMaxoResultSingleDisease makeRankedMaxoResultSingleDisease(
            TermId diseaseId,
            double meanScore,
            List<DifferentialDiagnosis> initialDiagnoses,
            List<List<DifferentialDiagnosis>> newMaxoDiagnosesList,
            DiscoverablePhenotypes discoverablePhenotypes,
            MdContext context) {

        // Step 1: Make disease and MAXO SimpleTerm
        String diseaseLabel = context.biometadataService().diseaseLabel(diseaseId).get();
        MySimpleTerm diseaseTerm = new MySimpleTerm(diseaseId, diseaseLabel);
        TermId maxoId = maxoHpoDiseaseRank.getMaxoId();
        String maxoLabel = maxoHpoDiseaseRank.getMaxoLabel();
        MySimpleTerm maxoTerm = new MySimpleTerm(maxoId, maxoLabel);

        // Step 2: Get RankedOmimTerm for disease of interest
        RankedOmimTerm rankedOmimTerm = getRankedOmimTerm(initialDiagnoses, newMaxoDiagnosesList, targetDiseaseId);

        // Step 2: Get all MAxO ascertained HPO Ids for single disease of interest
        Set<TermId> allMaxoAscertainedHpoIds = maxoHpoDiseaseRank.getAllMaxoAscertainedHpoIds();
        Set<TermId> diseasePhenotypeIds = discoverablePhenotypes.getDiscoverablePhenotypeIds(ppkt, diseaseId);
        allMaxoAscertainedHpoIds.retainAll(diseasePhenotypeIds);

        // Step 3: Compute nDiscoverablePhenotypes and Total IC
        int nMaxoDiseaseHpoTerms = allMaxoAscertainedHpoIds.size();
        Map<TermId, Double> termToIcMap = context.resources().termToIcMap();
        double totalIC = allMaxoAscertainedHpoIds.isEmpty() ? 0.0 :
                allMaxoAscertainedHpoIds.stream().mapToDouble(termToIcMap::get).sum();

        // Step 4: Construct Final Result
        return new RankedMaxoResultSingleDisease(diseaseTerm, maxoTerm,
                nMaxoDiseaseHpoTerms, totalIC, meanScore, rankedOmimTerm);
    }
}
