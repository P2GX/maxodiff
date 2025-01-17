package org.monarchinitiative.maxodiff.html.service;


import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.maxodiff.core.lirical.LiricalDifferentialDiagnosisEngineConfigurer;
import org.monarchinitiative.maxodiff.core.lirical.MaxodiffLiricalAnalysisRunner;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//TODO: remove?
@Deprecated(forRemoval = true)
public class LiricalInputService {

//    public static LiricalConfiguration liricalConfiguration(LiricalRecord liricalRecord) throws LiricalException {
//
//        return LiricalConfiguration.of(
//                liricalRecord.liricalDataDir(), liricalRecord.exomiserPath(),
//                liricalRecord.genomeBuild(),
//                liricalRecord.transcriptDatabase(),
//                liricalRecord.pathogenicityThreshold(),
//                liricalRecord.defaultVariantBackgroundFrequency(),
//                liricalRecord.strict(), liricalRecord.globalAnalysisMode());
//    }

    public static LiricalDifferentialDiagnosisEngineConfigurer configureLiricalConfigurer(MaxodiffLiricalAnalysisRunner analysisRunner) {

        return LiricalDifferentialDiagnosisEngineConfigurer.of(analysisRunner);
    }

    public static PretestDiseaseProbability pretestDiseaseProbability(Lirical lirical) {
        Map<TermId, Double> diseaseIdToPretestProba = new HashMap<>();
        Set<TermId> diseaseIds = lirical.phenotypeService().diseases().diseaseIds();
        int nTotalDiseases = diseaseIds.size();
        diseaseIds.forEach(id -> diseaseIdToPretestProba.put(id, 1./nTotalDiseases));

        return PretestDiseaseProbability.of(diseaseIdToPretestProba);
    }
}
