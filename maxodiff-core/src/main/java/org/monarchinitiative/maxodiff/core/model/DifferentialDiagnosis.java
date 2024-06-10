package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

public interface DifferentialDiagnosis {

    static DifferentialDiagnosis of(
            TermId diseaseId,
            double score,
            double lr
    ) {
        return new DifferentialDiagnosisImpl(diseaseId, score, lr);
    }

    /**
     *
     * @return The TermId of the disease.
     */
    TermId diseaseId();

    /**
     *
     * @return The differential diagnosis score value.
     */
    double score();

    /**
     *
     * @return The Likelihood Ratio.
     */
    double lr();
}
