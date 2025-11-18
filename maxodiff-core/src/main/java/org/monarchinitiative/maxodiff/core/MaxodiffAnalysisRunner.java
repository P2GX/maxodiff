package org.monarchinitiative.maxodiff.core;

import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementResults;
import org.monarchinitiative.maxodiff.core.model.PhenopacketData;
import org.monarchinitiative.maxodiff.core.model.RankMaxo;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementOptions;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;

public class MaxodiffAnalysisRunner {
    private final static Logger LOGGER = LoggerFactory.getLogger(MaxoDiffAnalysisResultRow.class);
    private final int nDiseases;
    private final int nRepetitions;
    private final DifferentialDiagnosisEngine engine;
    private final DiffDiagRefiner maxoDiffRefiner;
    private final BiometadataService biometadataService;

    public MaxodiffAnalysisRunner(
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

    public MaxodiffAnalysisResult analyzeSample(PhenopacketData ppktData) throws Exception {
        Sample sample = ppktData.getSample();
        List<DifferentialDiagnosis> differentialDiagnoses = engine.run(sample);
        // Get List of Refinement results: maxo term scores and frequencies
        RefinementOptions options = RefinementOptions.of(this.nDiseases, this.nRepetitions);
        List<DifferentialDiagnosis> orderedDiagnoses = maxoDiffRefiner.getOrderedDiagnoses(differentialDiagnoses, options);
        List<HpoDisease> diseases = maxoDiffRefiner.getDiseases(orderedDiagnoses);
        Map<TermId, List<HpoFrequency>> hpoTermCounts = maxoDiffRefiner.getHpoTermCounts(diseases);
        RefinementResults refinementResults = getRefinementResults(
                differentialDiagnoses, orderedDiagnoses, hpoTermCounts, sample);
        // refinement results already sorted in object
        var results = new ArrayList<>(refinementResults.maxodiffResults().stream().toList());
        return new MaxodiffAnalysisResult(results, hpoTermCounts);
    }

    public MaxoDiffAnalysisResultRow batchAnalysis(PhenopacketData ppktData) throws Exception {
        MaxodiffAnalysisResult result = analyzeSample(ppktData);
        List<MaxodiffResult> resultsList = result.results();
        String phenopacket_id = ppktData.sampleId();
        String disease_id = ppktData.diseaseIds().getFirst().getValue();
        MaxodiffResult topResult = resultsList.getFirst();
        String maxo_id = topResult.rankMaxoScore().maxoId().toString();
        String maxo_label = biometadataService.maxoLabel(maxo_id).orElse("unknown");
        double maxScoreValue = topResult.rankMaxoScore().maxoScore();
        Set<TermId> diseaseIds = topResult.rankMaxoScore().maxoOmimTermIds();

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


    private RefinementResults getRefinementResults(
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

        return maxoDiffRefiner.run(sample,
                orderedDiagnoses,
                options,
                rankMaxo,
                hpoTermCounts,
                maxoToHpoTermIdMap);
    }


}
