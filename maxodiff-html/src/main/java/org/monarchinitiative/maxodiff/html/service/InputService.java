package org.monarchinitiative.maxodiff.html.service;


import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.maxodiff.lirical.LiricalConfiguration;
import org.monarchinitiative.maxodiff.lirical.LiricalDifferentialDiagnosisEngineConfigurer;
import org.monarchinitiative.maxodiff.lirical.LiricalRecord;


public class InputService {

    public static LiricalDifferentialDiagnosisEngineConfigurer configureLiricalConfigurer(LiricalRecord liricalRecord) throws LiricalException {

        LiricalConfiguration liricalConfiguration = LiricalConfiguration.of(
                liricalRecord.liricalDataDir(), liricalRecord.exomiserPath(),
                liricalRecord.genomeBuild(),
                liricalRecord.transcriptDatabase(),
                liricalRecord.pathogenicityThreshold(),
                liricalRecord.defaultVariantBackgroundFrequency(),
                liricalRecord.strict(), liricalRecord.globalAnalysisMode());
        LiricalAnalysisRunner liricalAnalysisRunner = liricalConfiguration.lirical().analysisRunner();

        return LiricalDifferentialDiagnosisEngineConfigurer.of(liricalAnalysisRunner);
    }
}
