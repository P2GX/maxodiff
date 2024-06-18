package org.monarchinitiative.maxodiff.lirical;

import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;

/**
 * Configure {@link LiricalDifferentialDiagnosisEngine} for running LIRICAL analysis 
 * with specific parameter/option setup.
 */
public class LiricalDifferentialDiagnosisEngineConfigurer {

    private final LiricalAnalysisRunner liricalAnalysisRunner;
    private final AnalysisOptions analysisOptions;

    public LiricalDifferentialDiagnosisEngine configure() {
        return new LiricalDifferentialDiagnosisEngine(liricalAnalysisRunner(), analysisOptions());
    }

    public static LiricalDifferentialDiagnosisEngineConfigurer of(LiricalAnalysisRunner analysisRunner, AnalysisOptions analysisOptions) {
        return new LiricalDifferentialDiagnosisEngineConfigurer(analysisRunner, analysisOptions);
    }

    private LiricalDifferentialDiagnosisEngineConfigurer(LiricalAnalysisRunner analysisRunner, AnalysisOptions analysisOptions) {
        this.liricalAnalysisRunner = analysisRunner;
        this.analysisOptions = analysisOptions;
    }

    private LiricalAnalysisRunner liricalAnalysisRunner() {
        return liricalAnalysisRunner;
    }

    private AnalysisOptions analysisOptions() {
        return analysisOptions;
    }

}
