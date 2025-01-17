package org.monarchinitiative.maxodiff.core.lirical;

import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngineException;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class LiricalDifferentialDiagnosisEngine implements DifferentialDiagnosisEngine {

    private final AnalysisOptions options;
    private final MaxodiffLiricalAnalysisRunner maxodiffRunner;
    private final Set<TermId> diseaseIds;

    public LiricalDifferentialDiagnosisEngine(MaxodiffLiricalAnalysisRunner maxodiffRunner, AnalysisOptions options) {
        this.options = Objects.requireNonNull(options);
        this.maxodiffRunner = Objects.requireNonNull(maxodiffRunner);
        this.diseaseIds = null;
    }

    public LiricalDifferentialDiagnosisEngine(MaxodiffLiricalAnalysisRunner maxodiffRunner, AnalysisOptions options,
                                              Set<TermId> diseaseIds) {
        this.options = Objects.requireNonNull(options);
        this.maxodiffRunner = Objects.requireNonNull(maxodiffRunner);
        this.diseaseIds = diseaseIds;
    }

    public List<DifferentialDiagnosis> run(Sample sample) {

        // Get LIRICAL AnalysisData from sample
        AnalysisData analysisData = AnalysisData.of(sample.id(),
                null,
                Sex.UNKNOWN,
                sample.presentHpoTermIds(),
                sample.excludedHpoTermIds(),
                GenesAndGenotypes.empty());


        // Get LIRICAL AnalysisResults
        AnalysisResults results = null;
        try {
            if (diseaseIds == null) {
                results = maxodiffRunner.run(analysisData, options);
            } else if (diseaseIds != null) {
                results = maxodiffRunner.runWithTermIds(analysisData, options, diseaseIds);
            }
        } catch (LiricalAnalysisException e) {
            throw new DifferentialDiagnosisEngineException(e);
        }
        // Get Differential Diagnoses from LIRICAL AnalysisResults
        assert results != null;
        return results.resultsWithDescendingPostTestProbability()
                .map(tr -> DifferentialDiagnosis.of(tr.diseaseId(), tr.posttestProbability(), tr.getCompositeLR()))
                .toList();
    }

}
