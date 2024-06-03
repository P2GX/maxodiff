package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

record DifferentialDiagnosisModelImpl(
        TermId diseaseId,
        double score,
        double lr
) implements DifferentialDiagnosisModel {
}
