package org.monarchinitiative.maxodiff.core.diffdg;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;

import java.util.List;

public interface DifferentialDiagnosisEngine {

    List<DifferentialDiagnosis> run(Sample sample);

}
