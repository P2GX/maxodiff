package org.monarchinitiative.maxodiff.lirical;

import org.monarchinitiative.lirical.core.analysis.AnalysisData;
import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisException;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngineException;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;

public class LiricalDifferentialDiagnosisEngine implements DifferentialDiagnosisEngine {

    private final AnalysisOptions options;
    private final MaxodiffLiricalAnalysisRunner maxodiffRunner;

    public LiricalDifferentialDiagnosisEngine(MaxodiffLiricalAnalysisRunner maxodiffRunner, AnalysisOptions options) {
        this.options = Objects.requireNonNull(options);
        this.maxodiffRunner = Objects.requireNonNull(maxodiffRunner);
    }

    public List<DifferentialDiagnosis> run(Sample sample) {
        return run(sample, null);
    }

    public List<DifferentialDiagnosis> run(Sample sample, Collection<TermId> diseaseIds) {

        // Get LIRICAL AnalysisData from sample
        AnalysisData analysisData = AnalysisData.of(sample.id(),
                null,
                Sex.UNKNOWN,
                sample.presentHpoTermIds(),
                sample.excludedHpoTermIds(),
                GenesAndGenotypes.empty());


        Set<TermId> diseaseIdsSet = new HashSet<>(diseaseIds);
        // Get LIRICAL AnalysisResults
        AnalysisResults results = getLiricalAnalysisResults(analysisData, diseaseIdsSet);
        // Get Differential Diagnoses from LIRICAL AnalysisResults
        assert results != null;
        return results.resultsWithDescendingPostTestProbability()
                .map(tr -> DifferentialDiagnosis.of(tr.diseaseId(), tr.posttestProbability(), tr.getCompositeLR()))
                .toList();
    }

    private AnalysisResults getLiricalAnalysisResults(AnalysisData analysisData, Set<TermId> diseaseIds) {
        try {
            if (diseaseIds == null) {
                return maxodiffRunner.run(analysisData, options);
            } else {
                return maxodiffRunner.runWithTermIds(analysisData, options, diseaseIds);
            }
        } catch (LiricalAnalysisException e) {
            throw new DifferentialDiagnosisEngineException(e);
        }
    }

}
