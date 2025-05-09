package org.monarchinitiative.maxodiff.core.analysis.refinement;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class MaxoDiffRankRefiner extends BaseDiffDiagRefiner {

    public MaxoDiffRankRefiner(HpoDiseases hpoDiseases, Map<TermId, Set<TermId>> fullHpoToMaxoIdTermMap,
                               Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap, MinimalOntology minHpo, Ontology hpo) {
        super(hpoDiseases, fullHpoToMaxoIdTermMap, hpoToMaxoTermMap, minHpo, hpo);
    }


    @Override
    public RefinementResults run(Sample sample,
                                 Collection<DifferentialDiagnosis> differentialDiagnoses,
                                 RefinementOptions options,
                                 DifferentialDiagnosisEngine engine,
                                 Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                                 Map<TermId, List<HpoFrequency>> hpoTermCounts,
                                 Map<TermId, List<DifferentialDiagnosis>> maxoTermToDDEngineDiagnosesMap) {

        // Calculate final ranks and make list of MaxodiffResult objects.
        List<MaxodiffResult> maxodiffResultsList = new ArrayList<>();
        for (TermId maxoId : maxoToHpoTermIdMap.keySet()) {
            List<DifferentialDiagnosis> maxoTermDiagnoses = null;
            // Get the set of HPO terms that can be ascertained by the MAXO term
            Set<TermId> hpoTermIds = maxoToHpoTermIdMap.get(maxoId);
            // Get MAXO term differential diagnoses, if available
            maxoTermDiagnoses = maxoTermToDDEngineDiagnosesMap.get(maxoId);
            // Calculate MAXO term differential diagnoses, if needed
            if (maxoTermDiagnoses == null) {
                Integer nDiseases = options.nDiseases();
                maxoTermDiagnoses = AnalysisUtils.getMaxoTermDifferentialDiagnoses(sample, hpoTermIds, engine, nDiseases);
            }
            // Calculate final rank and make rank record
            MaxoTermScore maxoTermScore = AnalysisUtils.getMaxoTermRankRecord(hpoTermIds,
                    maxoId,
                    differentialDiagnoses.stream().toList(),
                    maxoTermDiagnoses,
                    options);
            // Get HPO frequency records
            List<Frequencies> frequencies = getFrequencyRecords(maxoTermScore.omimTermIds(),
                    maxoTermScore.hpoTermIds(), hpoTermCounts);
            List<Frequencies> maxoFrequencies = getFrequencyRecords(maxoTermScore.maxoOmimTermIds(),
                    maxoTermScore.hpoTermIds(), hpoTermCounts);
            // Make dummy RankMaxoScore record
            RankMaxoScore rankMaxoScore = new RankMaxoScore(maxoId, maxoTermScore.omimTermIds(), maxoTermScore.maxoOmimTermIds(),
                    Set.of(), maxoTermScore.scoreDiff(), List.of(), Map.of(), Map.of(),0, 0);
            // Make MaxodiffResult for the MAXO term
            MaxodiffResult maxodiffResult = new MaxodiffResultImpl(maxoTermScore, rankMaxoScore, frequencies, maxoFrequencies);
            maxodiffResultsList.add(maxodiffResult);
        }

        // Return RefinementResults object, which contains the list of MaxodiffResult objects.
        return new RefinementResultsImpl(maxodiffResultsList);
    }

}
