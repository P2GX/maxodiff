package org.monarchinitiative.maxodiff.html.analysis;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;

import java.util.List;


public record InputRecord(Sample sample, List<DifferentialDiagnosis> differentialDiagnoses) {}

