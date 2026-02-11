package org.monarchinitiative.maxodiff.core;

import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.RankedMaxoResult;
import org.monarchinitiative.maxodiff.core.analysis.RankedOmimTerm;
import org.monarchinitiative.maxodiff.core.analysis.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.refinement.*;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.PhenopacketData;
import org.monarchinitiative.maxodiff.core.model.RankMaxo;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class MaxodiffAnalysisRunner1 {
    private final static Logger LOGGER = LoggerFactory.getLogger(MaxoDiffAnalysisResultRow.class);
    private final int nDiseases;
    private final int nRepetitions;
    private final DifferentialDiagnosisEngine engine;
    private final DiffDiagRefiner maxoDiffRefiner;
    private final BiometadataService biometadataService;

    public MaxodiffAnalysisRunner1(
            int nDiseases,
            int nRepetitions,
            DifferentialDiagnosisEngine engine,
            DiffDiagRefiner maxoDiffRefiner,
            BiometadataService biometadataService
    ) {
        this.nDiseases = nDiseases;
        this.nRepetitions = nRepetitions;
        this.engine = engine;
        this.maxoDiffRefiner = maxoDiffRefiner;
        this.biometadataService = biometadataService;
    }


    public List<RankedMaxoResult> analyzeSample(PhenopacketData ppktData) throws Exception {
        Sample sample = ppktData.getSample();
        List<DifferentialDiagnosis> differentialDiagnoses = engine.run(sample);
        // Get List of Refinement results: maxo term scores and frequencies
        RefinementOptions options = RefinementOptions.of(this.nDiseases, this.nRepetitions);
        List<DifferentialDiagnosis> orderedDiagnoses = maxoDiffRefiner.getOrderedDiagnoses(differentialDiagnoses, options);
        List<HpoDisease> diseases = maxoDiffRefiner.getDiseases(orderedDiagnoses);
        Map<TermId, List<HpoFrequency>> hpoTermCounts = maxoDiffRefiner.getHpoTermCounts(diseases);
        return getRefinementResults(differentialDiagnoses, orderedDiagnoses, hpoTermCounts, sample);
    }

    public MaxoDiffAnalysisResultRow batchAnalysis(PhenopacketData ppktData) throws Exception {
        List<RankedMaxoResult> resultsList = analyzeSample(ppktData);
        String phenopacket_id = ppktData.sampleId();
        String disease_id = ppktData.diseaseIds().getFirst().getValue();
        RankedMaxoResult topResult = resultsList.getFirst();
        String maxo_id = topResult.maxoTerm().termId();
        String maxo_label = topResult.maxoTerm().termLabel();
        double maxScoreValue = topResult.maxoScore();
        List<RankedOmimTerm> rankedOmimTermList = topResult.rankedOmimTermList();
        List<String> diseaseIdsStr = rankedOmimTermList.stream().map(RankedOmimTerm::omimTerm).map(SimpleTerm::termId).toList();
        Set<TermId> diseaseIds = diseaseIdsStr.stream().map(TermId::of).collect(Collectors.toSet());

        return new MaxoDiffAnalysisResultRow(
                phenopacket_id,
                disease_id,
                maxo_id,
                maxo_label,
                this.nDiseases,
                diseaseIds,
                this.nRepetitions,
                maxScoreValue
        );
    }


    private List<RankedMaxoResult> getRefinementResults(
            List<DifferentialDiagnosis> differentialDiagnoses,
            List<DifferentialDiagnosis> orderedDiagnoses,
            Map<TermId, List<HpoFrequency>> hpoTermCounts,
            Sample sample) throws Exception {
        RefinementOptions options = RefinementOptions.of(this.nDiseases, nRepetitions);
        List<DifferentialDiagnosis> allOrderedDiagnoses = differentialDiagnoses.stream()
                .sorted(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed())
                .toList();

        List<DifferentialDiagnosis> initialDiagnoses = orderedDiagnoses.subList(0, options.nDiseases());
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = maxoDiffRefiner.getMaxoToHpoTermIdMap(hpoTermCounts);

        String diseaseProbModel = "ranked"; // TODO -- Can we make this an enum?
        RankMaxo rankMaxo = maxoDiffRefiner.getRankMaxo(allOrderedDiagnoses, initialDiagnoses, engine, maxoToHpoTermIdMap, diseaseProbModel);

        return maxoDiffRefiner.runNew(sample,
                orderedDiagnoses,
                options,
                rankMaxo,
                hpoTermCounts,
                maxoToHpoTermIdMap,
                biometadataService);
    }


}
