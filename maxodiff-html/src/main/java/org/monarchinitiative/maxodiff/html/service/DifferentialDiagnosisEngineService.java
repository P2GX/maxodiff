package org.monarchinitiative.maxodiff.html.service;

import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface DifferentialDiagnosisEngineService {

    //TODO: implement

    static DifferentialDiagnosisEngineService empty() {
        return DifferentialDiagnosisEngineServiceImpl.empty();
    }

    static DifferentialDiagnosisEngineService of(Map<String, DifferentialDiagnosisEngine> engineMap) {
        return DifferentialDiagnosisEngineServiceImpl.of(engineMap);
    }

    Set<String> getEngineNames();

    Optional<DifferentialDiagnosisEngine> getEngine(String engineName);
}
