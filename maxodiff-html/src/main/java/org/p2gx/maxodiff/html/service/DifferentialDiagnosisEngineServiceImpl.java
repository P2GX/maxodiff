package org.p2gx.maxodiff.html.service;

import org.p2gx.maxodiff.core.diffdg.DDxEngine;

import java.util.*;

public class DifferentialDiagnosisEngineServiceImpl implements DifferentialDiagnosisEngineService {

    private final Map<String, DDxEngine> engineMap;

    private static final DifferentialDiagnosisEngineServiceImpl EMPTY = new DifferentialDiagnosisEngineServiceImpl(Map.of());

    static DifferentialDiagnosisEngineServiceImpl empty() {
        return EMPTY;
    }

    public static DifferentialDiagnosisEngineServiceImpl of(Map<String, DDxEngine> engineMap) {
        return new DifferentialDiagnosisEngineServiceImpl(engineMap);
    }

    private DifferentialDiagnosisEngineServiceImpl(Map<String, DDxEngine> engineMap) {
        this.engineMap = engineMap;
    }

    public Set<String> getEngineNames() {
        return engineMap.keySet();
    }

    public Optional<DDxEngine> getEngine(String engineName) {
        return Optional.ofNullable(engineMap.get(engineName));
    }

}
