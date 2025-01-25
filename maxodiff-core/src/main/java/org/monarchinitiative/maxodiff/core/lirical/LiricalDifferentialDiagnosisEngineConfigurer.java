package org.monarchinitiative.maxodiff.core.lirical;

import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;

/**
 * Configure {@link LiricalDifferentialDiagnosisEngine} for running LIRICAL analysis 
 * with specific parameter/option setup.
 */
public class LiricalDifferentialDiagnosisEngineConfigurer {

    private LiricalAnalysisRunner liricalAnalysisRunner;
    private final MaxodiffLiricalAnalysisRunner maxodiffLiricalAnalysisRunner;

    public static LiricalDifferentialDiagnosisEngineConfigurer of(MaxodiffLiricalAnalysisRunner maxodiffLiricalAnalysisRunner) {
        return new LiricalDifferentialDiagnosisEngineConfigurer(maxodiffLiricalAnalysisRunner);
    }

    private LiricalDifferentialDiagnosisEngineConfigurer(MaxodiffLiricalAnalysisRunner maxodiffLiricalAnalysisRunner) {
        this.maxodiffLiricalAnalysisRunner = maxodiffLiricalAnalysisRunner;
    }

    public LiricalDifferentialDiagnosisEngine configure(AnalysisOptions options) {
        return new LiricalDifferentialDiagnosisEngine(maxodiffLiricalAnalysisRunner, options);
    }

//    public LiricalDifferentialDiagnosisEngine configure(AnalysisOptions options, Set<TermId> diseaseIds) {
//        return new LiricalDifferentialDiagnosisEngine(maxodiffLiricalAnalysisRunner, options, diseaseIds);
//    }
   
}
