package org.p2gx.maxodiff.core;

import org.p2gx.maxodiff.core.analysis.*;
import org.p2gx.maxodiff.core.io.MdContext;

import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;
import org.p2gx.maxodiff.core.model.PhenopacketData;
import org.p2gx.maxodiff.core.model.RankMaxo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class MaxodiffAnalysisRunner {
    private final static Logger LOGGER = LoggerFactory.getLogger(MaxodiffAnalysisRunner.class);
    private final MdContext mdContext;
    private final DDxEngine engine;
    private final DiffDiagRefiner maxoDiffRefiner;
    private final List<HpoFrequency> hpoFrequencies;

    public MaxodiffAnalysisRunner(
            MdContext mdContext,
            DDxEngine engine,
            DiffDiagRefiner maxoDiffRefiner,
            List<HpoFrequency> hpoFrequencies
    ) {
        this.mdContext = mdContext;
        this.engine = engine;
        this.maxoDiffRefiner = maxoDiffRefiner;
        this.hpoFrequencies = hpoFrequencies;
    }


    public List<RankedMaxoResult> analyzeSample(PhenopacketData ppktData) throws Exception {
        LOGGER.info("Analyzing " + ppktData.sampleId());
        List<TermId> ppktMaxoIds = ppktData.maxoProcedureIds();
        List<DifferentialDiagnosis> differentialDiagnoses = engine.run(ppktData);
        // Get List of Refinement results: maxo term scores and frequencies
       // RefinementOptions options = RefinementOptions.of(this.nDiseases, this.nRepetitions);
        List<DifferentialDiagnosis> orderedDiagnoses = maxoDiffRefiner.getOrderedDiagnoses(differentialDiagnoses);
        List<HpoDisease> diseases = maxoDiffRefiner.getDiseases(orderedDiagnoses);
        List<HpoFrequency> hpoFrequenciesNDiseases = maxoDiffRefiner.getHpoFrequenciesNDiseases(diseases, hpoFrequencies);
        return getRefinementResults(differentialDiagnoses, orderedDiagnoses, hpoFrequenciesNDiseases, ppktData, ppktMaxoIds);
    }

    public List<RankedMaxoResultSingleDisease> analyzeSampleSingleDisease(PhenopacketData ppktData, TermId targetDiseaseId) throws Exception {
        List<TermId> ppktMaxoIds = ppktData.maxoProcedureIds();
        List<DifferentialDiagnosis> differentialDiagnoses = engine.run(ppktData);
        // Get List of Refinement results: maxo term scores and frequencies
        // RefinementOptions options = RefinementOptions.of(this.nDiseases, this.nRepetitions);
        List<DifferentialDiagnosis> orderedDiagnoses = maxoDiffRefiner.getOrderedDiagnoses(differentialDiagnoses);
        List<HpoDisease> diseases = maxoDiffRefiner.getDiseases(orderedDiagnoses);
        List<HpoFrequency> hpoFrequenciesNDiseases = maxoDiffRefiner.getHpoFrequenciesNDiseases(diseases, hpoFrequencies);
        return getRefinementResultsSingleDisease(differentialDiagnoses, orderedDiagnoses, hpoFrequenciesNDiseases, ppktData, targetDiseaseId);
    }

    public MaxoDiffAnalysisResultRow batchAnalysis(PhenopacketData ppktData) throws Exception {
        List<RankedMaxoResult> resultsList = analyzeSample(ppktData);
        String phenopacket_id = ppktData.sampleId();
        String disease_id = ppktData.diseaseIds().getFirst().getValue();
        RankedMaxoResult topResult = resultsList.getFirst();
        TermId maxo_id = topResult.maxoTerm().tid();
        String maxo_label = topResult.maxoTerm().label();
        double maxScoreValue = topResult.maxoScore();
        List<RankedOmimTerm> rankedOmimTermList = topResult.rankedOmimTermList();
        List<TermId> diseaseIdsStr = rankedOmimTermList.stream().map(RankedOmimTerm::omimTerm).map(SimpleTerm::tid).toList();
        Set<TermId> diseaseIds = new HashSet<>(diseaseIdsStr);

        return new MaxoDiffAnalysisResultRow(
                phenopacket_id,
                disease_id,
                maxo_id.getValue(),
                maxo_label,
                this.mdContext.params().nDiseases(),
                diseaseIds,
                this.mdContext.params().nRepetitions(),
                maxScoreValue
        );
    }


    private List<RankedMaxoResult> getRefinementResults(
            List<DifferentialDiagnosis> differentialDiagnoses,
            List<DifferentialDiagnosis> orderedDiagnoses,
            List<HpoFrequency> hpoFrequenciesNDiseases,
            PhenopacketData sample,
            List<TermId> ppktMaxoIds) throws Exception {
        List<DifferentialDiagnosis> allOrderedDiagnoses = differentialDiagnoses.stream()
                .sorted(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed())
                .toList();
        List<DifferentialDiagnosis> initialNDiagnoses = orderedDiagnoses.subList(0, this.mdContext.params().nDiseases());
        Set<TermId> initialNDiagnosesIds = initialNDiagnoses.stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LOGGER.debug("Making MAxO:HPO Term Id Map");
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = MaxoHpoTermIdMaps.getMaxoToHpoTermIdMap(this.mdContext.resources().maxoAnnotsMap());//maxoDiffRefiner.getMaxoToHpoTermIdMap(hpoFrequenciesNDiseases);
        LOGGER.debug("Making RankMaxo object");
        RankMaxo rankMaxo = maxoDiffRefiner.getRankMaxo(allOrderedDiagnoses, initialNDiagnoses, engine,
                maxoToHpoTermIdMap, hpoFrequenciesNDiseases);

        return maxoDiffRefiner.run(sample,
                initialNDiagnosesIds,
                rankMaxo);
    }

    private List<RankedMaxoResultSingleDisease> getRefinementResultsSingleDisease(
            List<DifferentialDiagnosis> differentialDiagnoses,
            List<DifferentialDiagnosis> orderedDiagnoses,
            List<HpoFrequency> hpoFrequenciesNDiseases,
            PhenopacketData sample,
            TermId targetDiseaseId) throws Exception {
        List<DifferentialDiagnosis> allOrderedDiagnoses = differentialDiagnoses.stream()
                .sorted(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed())
                .toList();
        List<DifferentialDiagnosis> initialNDiagnoses = orderedDiagnoses.subList(0, this.mdContext.params().nDiseases());
        Set<TermId> initialNDiagnosesIds = initialNDiagnoses.stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = MaxoHpoTermIdMaps.getMaxoToHpoTermIdMap(this.mdContext.resources().maxoAnnotsMap());//maxoDiffRefiner.getMaxoToHpoTermIdMap(hpoFrequenciesNDiseases);
        RankMaxo rankMaxo = maxoDiffRefiner.getRankMaxo(allOrderedDiagnoses, initialNDiagnoses, engine,
                maxoToHpoTermIdMap, hpoFrequenciesNDiseases);

        return maxoDiffRefiner.runSingleDisease(sample,
                targetDiseaseId,
                initialNDiagnosesIds,
                rankMaxo);
    }


}
