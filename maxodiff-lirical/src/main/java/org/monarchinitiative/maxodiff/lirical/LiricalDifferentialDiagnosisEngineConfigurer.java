package org.monarchinitiative.maxodiff.lirical;

import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;

/**
 * Configure {@link LiricalDifferentialDiagnosisEngine} for running LIRICAL analysis 
 * with specific parameter/option setup.
 */
public class LiricalDifferentialDiagnosisEngineConfigurer {

    private final LiricalAnalysisRunner liricalAnalysisRunner;

    public static LiricalDifferentialDiagnosisEngineConfigurer of(LiricalAnalysisRunner analysisRunner) {
        return new LiricalDifferentialDiagnosisEngineConfigurer(analysisRunner);
    }
    
    private LiricalDifferentialDiagnosisEngineConfigurer(LiricalAnalysisRunner analysisRunner) {
        this.liricalAnalysisRunner = analysisRunner;
    }

    public LiricalDifferentialDiagnosisEngine configure(AnalysisOptions options) {
        return new LiricalDifferentialDiagnosisEngine(liricalAnalysisRunner, options);
    }
   
}
