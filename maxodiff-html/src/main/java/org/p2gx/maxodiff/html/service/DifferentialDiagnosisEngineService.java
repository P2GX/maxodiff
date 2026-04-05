package org.p2gx.maxodiff.html.service;

import org.p2gx.maxodiff.core.diffdg.DDxEngine;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface DifferentialDiagnosisEngineService {

    //TODO: implement

    static DifferentialDiagnosisEngineService empty() {
        return DifferentialDiagnosisEngineServiceImpl.empty();
    }

    static DifferentialDiagnosisEngineService of(Map<String, DDxEngine> engineMap) {
        return DifferentialDiagnosisEngineServiceImpl.of(engineMap);
    }

    Set<String> getEngineNames();

    Optional<DDxEngine> getEngine(String engineName);
}
