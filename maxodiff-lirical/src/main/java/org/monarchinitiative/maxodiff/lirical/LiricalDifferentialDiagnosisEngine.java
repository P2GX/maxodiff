package org.monarchinitiative.maxodiff.lirical;

import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;

import java.util.List;

public class LiricalDifferentialDiagnosisEngine implements DifferentialDiagnosisEngine {

    private final AnalysisOptions options;
    private final LiricalAnalysisRunner runner;

    public LiricalDifferentialDiagnosisEngine(LiricalAnalysisRunner runner, AnalysisOptions options) {
        this.options = options;
        this.runner = runner;
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
        // TODO: we are not allowed to throw a checked exception by `DifferentialDiagnosisEngine`.
        // Let's re-throw `LiricalAnalysisException` as our own unchecked exception 
        // (you may need to create one, in `maxodiff-core`).
        AnalysisResults results = runner.run(analysisData, options);
        // Get Differential Diagnoses from LIRICAL AnalysisResults
        return results.resultsWithDescendingPostTestProbability()
                .map(tr -> DifferentialDiagnosis.of(tr.diseaseId(), tr.posttestProbability(), tr.getCompositeLR()))
                .toList();
    }

}
