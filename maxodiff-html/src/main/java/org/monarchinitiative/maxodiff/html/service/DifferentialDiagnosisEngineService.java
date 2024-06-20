package org.monarchinitiative.maxodiff.html.service;

import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;

import java.util.List;
import java.util.Optional;

public interface DifferentialDiagnosisEngineService {

    //TODO: implement
    List<String> getEngineNames();

    Optional<DifferentialDiagnosisEngine> getEngine(String engineName);
}
