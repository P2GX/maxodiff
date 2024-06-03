package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;

record SimpleSample(
        String id,
        Collection<TermId> presentHpoTermIds,
        Collection<TermId> excludedHpoTermIds,
        Collection<DifferentialDiagnosisModel> differentialDiagnoses
) implements Sample {
}
