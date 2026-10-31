package org.p2gx.maxodiff.core.analysis;

import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.p2gx.maxodiff.core.model.*;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;


public class MaxoTermEvaluator implements Callable<RankedMaxoResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaxoTermEvaluator.class);

    private final MaxoHpoDiseaseRank maxoHpoDiseaseRank;
    private final int nRepetitions;
    private final PhenopacketData ppkt;
    private final DDxEngine engine;
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
        this.diseaseIds = diseaseIds;
        this.initialDiagnoses = initialDiagnoses;
        this.biometadataService = biometadataService;
        this.hpoFrequenciesNDiseases = hpoFrequenciesNDiseases;
    }

    @Override
    public RankedMaxoResult call() {
        WeightedHpoSampler sampler = new WeightedHpoSampler(maxoHpoDiseaseRank.getHpoWeights());
        Random random = new Random();
        MaxoSimulation simulation = new MaxoSimulation(
                sampler,
                ppkt,
                engine,
                diseaseIds,
                initialDiagnoses,
                id -> biometadataService.hpoLabel(id).orElse("unknown"));
        SimulationOutcome outcome = simulation.run(maxoHpoDiseaseRank.getSampledHpoCounts(nRepetitions, random), random);
        return makeRankedMaxoResult(outcome.selectionCounts(), outcome.meanScore(), initialDiagnoses,
                outcome.newDiagnoses(), hpoFrequenciesNDiseases);
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
            SimpleTerm omimTerm = new SimpleTerm(diseaseId, diseaseLabel);
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
     * @param omimIds disease Ids.
     * @param hpoIds hpo Ids.
     * @param hpoTermFrequencies Map of HPO Term Id and List of HpoFrequency objects.
     * @return List of Frequencies records
     */
    private List<HpoFrequency> getFrequencyRecords(Set<TermId> omimIds, Set<SimpleTerm> hpoIds,
                                                  List<HpoFrequency> hpoTermFrequencies) {

        List<HpoFrequency> frequencyRecords = new ArrayList<>();
        //Set<TermId> omimIds = maxoTermScoreRecord.omimTermIds();
        for (SimpleTerm hpoTerm : hpoIds) { //maxoTermScoreRecord.hpoTermIds()
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
            Map<SimpleTerm, Integer> selectionCounts,
            double meanScore,
            List<DifferentialDiagnosis> initialDiagnoses,
            List<List<DifferentialDiagnosis>> newMaxoDiagnosesList,
            List<HpoFrequency> hpoFrequenciesNDiseases) {

        LOGGER.debug("Making final results list");
        // Step 1: Make MAXO SimpleTerm
        TermId maxoId = maxoHpoDiseaseRank.getMaxoId();
        String maxoLabel = maxoHpoDiseaseRank.getMaxoLabel();
        SimpleTerm maxoTerm = new SimpleTerm(maxoId, maxoLabel);

        // Step 2: get OMIMs from the initial Phenomizer plus simulation with ranks (List<RankedOmimTerm>)
        Set<TermId> maxoIds = extractDiseaseIds(newMaxoDiagnosesList.getFirst());
        List<RankedOmimTerm> rankedOmimTermList = getRankedOmimTermList(initialDiagnoses, newMaxoDiagnosesList, maxoIds);

        // Step 3: get set of observed HPO terms discoverable by the MAxO term (List<CountedHpoTerm>)
        List<CountedHpoTerm> countedHpoTerms = selectionCounts.entrySet().stream()
                .map(e -> new CountedHpoTerm(e.getKey(), e.getValue()))
                .toList();

        // Step 4: get collection of HPO Term Frequencies (List<Frequencies>)
        List<HpoFrequency> frequencies = getFrequencyRecords(maxoIds, selectionCounts.keySet(), hpoFrequenciesNDiseases);

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
