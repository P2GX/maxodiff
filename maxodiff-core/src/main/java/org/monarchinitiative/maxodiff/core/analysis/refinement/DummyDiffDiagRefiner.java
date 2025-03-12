package org.monarchinitiative.maxodiff.core.analysis.refinement;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class DummyDiffDiagRefiner extends BaseDiffDiagRefiner {

    public DummyDiffDiagRefiner(HpoDiseases hpoDiseases,
                                Map<TermId, Set<TermId>> fullHpoToMaxoTermIdMap,
                                Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap,
                                MinimalOntology hpo) {
        super(hpoDiseases, fullHpoToMaxoTermIdMap, hpoToMaxoTermMap, hpo);
    }

    public RefinementResults run(Sample sample,
                                 Collection<DifferentialDiagnosis> differentialDiagnoses,
                                 RefinementOptions options,
                                 DifferentialDiagnosisEngine engine,
                                 Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                                 Map<TermId, List<HpoFrequency>> hpoTermCounts,
                                 Map<TermId, List<DifferentialDiagnosis>> maxoTermToDDEngineDiagnosesMap) {
        // Potential outline:
        // 1) pick MAXO term at random from full MAXO:HPO map
        // 2) get set of HPO terms from MAXO term
        // 3) make HPO combos and calculate score as normal?

        // Get list of Hpo diseases
        List<HpoDisease> diseases = getDiseases(differentialDiagnoses.stream().toList());
        // Calculate final scores and make list of MaxodiffResult objects.
        List<MaxodiffResult> maxodiffResultsList = new ArrayList<>();
        Random randomizer = new Random();
        int randomInt = randomizer.nextInt(maxoToHpoTermIdMap.size());
        System.out.println("random Int = " + randomInt);
        TermId maxoId = maxoToHpoTermIdMap.keySet().stream().toList().get(randomInt);
//        for (TermId maxoId : maxoToHpoTermIdMap.keySet()) {
        // Get the set of HPO terms that can be ascertained by the MAXO term
        Set<TermId> hpoTermIds = maxoToHpoTermIdMap.get(maxoId);
        // Get HPO term combinations
        List<List<TermId>> hpoCombos = AnalysisUtils.getHpoTermCombos(maxoToHpoTermIdMap, maxoId);
        // Calculate final score and make score record
        MaxoTermScore maxoTermScore = AnalysisUtils.getMaxoTermScoreRecord(hpoTermIds,
                hpoCombos,
                maxoId,
                differentialDiagnoses.stream().toList(),
                diseases,
                options);
        // Get HPO frequency records
//            List<Frequencies> frequencies = getFrequencyRecords(maxoTermScore, hpoTermCounts);
        // Make dummy RankMaxoScore record
        RankMaxoScore rankMaxoScore = new RankMaxoScore(maxoId, maxoTermScore.omimTermIds(), maxoTermScore.maxoOmimTermIds(),
                Set.of(), maxoTermScore.scoreDiff(), List.of(), Map.of());
        // Make MaxodiffResult for the MAXO term
        MaxodiffResult maxodiffResult = new MaxodiffResultImpl(maxoTermScore, rankMaxoScore, List.of(), List.of());
        maxodiffResultsList.add(maxodiffResult);
//        }

        // Return RefinementResults object, which contains the list of MaxodiffResult objects.
        return new RefinementResultsImpl(maxodiffResultsList);
    }
}
