package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

//TODO: rename this to CandidateDiagnosis, and make new class DifferentialDiagnosis that contains list of CandidateDiagnosis
record DifferentialDiagnosisImpl(
        TermId diseaseId,
        double score,
        double lr
) implements DifferentialDiagnosis {
}
