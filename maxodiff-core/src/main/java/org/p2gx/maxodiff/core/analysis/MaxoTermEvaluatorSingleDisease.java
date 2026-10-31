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
import java.util.stream.Collectors;


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
        if (nRepetitions < 1) {
            throw new IllegalArgumentException("nRepetitions must be at least 1 but was " + nRepetitions);
        }
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
        DiscoverablePhenotypes discoverablePhenotypes = maxoHpoTermProbabilities.getDiscoverablePhenotypes();
        Random random = new Random();
        MaxoSimulation simulation = new MaxoSimulation(
                new WeightedHpoSampler(maxoHpoDiseaseRank.getHpoWeights()),
                ppkt, engine, diseaseIds, initialDiagnoses,
                id -> biometadataService.hpoLabel(id).orElse("unknown"));
        SimulationOutcome outcome =
                simulation.run(maxoHpoDiseaseRank.getSampledHpoCounts(nRepetitions, random), random);
        return makeRankedMaxoResultSingleDisease(targetDiseaseId, outcome.meanScore(),
                initialDiagnoses, outcome.newDiagnoses(), discoverablePhenotypes, context);
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
        SimpleTerm diseaseTerm = new SimpleTerm(targetDiseaseId, diseaseLabel);
       return DiseaseRankSummary.summarize(diseaseTerm, initialDiagnoses, newMaxoDiagnosesList);
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
        SimpleTerm diseaseTerm = new SimpleTerm(diseaseId, diseaseLabel);
        TermId maxoId = maxoHpoDiseaseRank.getMaxoId();
        String maxoLabel = maxoHpoDiseaseRank.getMaxoLabel();
        SimpleTerm maxoTerm = new SimpleTerm(maxoId, maxoLabel);

        // Step 2: Get RankedOmimTerm for disease of interest
        RankedOmimTerm rankedOmimTerm = getRankedOmimTerm(initialDiagnoses, newMaxoDiagnosesList, targetDiseaseId);

        // Step 2: Get all MAxO ascertained HPO Ids for single disease of interest
        Set<TermId> allMaxoAscertainedHpoIds = new HashSet<>(maxoHpoDiseaseRank.getAllMaxoAscertainedHpoIds());
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
