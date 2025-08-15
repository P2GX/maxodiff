package org.monarchinitiative.maxodiff.html.service;

import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;

import java.util.*;

public class DifferentialDiagnosisEngineServiceImpl implements DifferentialDiagnosisEngineService {

    private final Map<String, DifferentialDiagnosisEngine> engineMap;

    private static final DifferentialDiagnosisEngineServiceImpl EMPTY = new DifferentialDiagnosisEngineServiceImpl(Map.of());

    static DifferentialDiagnosisEngineServiceImpl empty() {
        return EMPTY;
    }

    public static DifferentialDiagnosisEngineServiceImpl of(Map<String, DifferentialDiagnosisEngine> engineMap) {
        return new DifferentialDiagnosisEngineServiceImpl(engineMap);
    }

    private DifferentialDiagnosisEngineServiceImpl(Map<String, DifferentialDiagnosisEngine> engineMap) {
        this.engineMap = engineMap;
    }

    public Set<String> getEngineNames() {
        return engineMap.keySet();
    }

    public Optional<DifferentialDiagnosisEngine> getEngine(String engineName) {
        return Optional.ofNullable(engineMap.get(engineName));
    }

}
