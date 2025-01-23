package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class MaxoDiffKolmogorovSmirnovRefiner extends BaseDiffDiagRefiner {

    public MaxoDiffKolmogorovSmirnovRefiner(HpoDiseases hpoDiseases, Map<TermId, Set<TermId>> fullHpoToMaxoTermIdMap,
                                            Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap, MinimalOntology hpo) {
        super(hpoDiseases, fullHpoToMaxoTermIdMap, hpoToMaxoTermMap, hpo);
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
                Integer nDiseases = 100;
                maxoTermDiagnoses = AnalysisUtils.getMaxoTermDifferentialDiagnoses(sample, hpoTermIds, engine, nDiseases);
            }
            // Calculate final score and make score record
            MaxoTermScore maxoTermScore = AnalysisUtils.getMaxoTermKolmogorovSmirnovRecord(hpoTermIds,
                    maxoId,
                    differentialDiagnoses.stream().toList(),
                    maxoTermDiagnoses,
                    options);
            // Get HPO frequency records
            List<Frequencies> frequencies = getFrequencyRecords(maxoTermScore.omimTermIds(),
                    maxoTermScore.hpoTermIds(), hpoTermCounts);
            List<Frequencies> maxoFrequencies = getFrequencyRecords(maxoTermScore.maxoOmimTermIds(),
                    maxoTermScore.hpoTermIds(), hpoTermCounts);
            // Make MaxodiffResult for the MAXO term
            MaxodiffResult maxodiffResult = new MaxodiffResultImpl(maxoTermScore, frequencies, maxoFrequencies);
            if (maxoTermScore.scoreDiff() != 0.0) {
                maxodiffResultsList.add(maxodiffResult);
            }

        }

        // Return RefinementResults object, which contains the list of MaxodiffResult objects.
        return new RefinementResultsImpl(maxodiffResultsList);
    }

    @Override
    public List<DifferentialDiagnosis> getOrderedDiagnoses(Collection<DifferentialDiagnosis> originalDifferentialDiagnoses,
                                                           RefinementOptions options) {
        if (originalDifferentialDiagnoses.size() < options.nDiseases()) {
            //TODO: replace with MaxodiffRuntimeException that extends RuntimeException.
            throw new RuntimeException("Input No. Diseases larger than No. diseases in sample.");
        }
        List<DifferentialDiagnosis> orderedDiagnoses = originalDifferentialDiagnoses.stream()
                .sorted(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed())
                .toList();

        return orderedDiagnoses.subList(0, 100); //options.nDiseases()
    }
}
