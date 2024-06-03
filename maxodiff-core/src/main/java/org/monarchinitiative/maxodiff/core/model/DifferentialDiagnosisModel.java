package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

public interface DifferentialDiagnosisModel {

    static DifferentialDiagnosisModel of(
            TermId diseaseId,
            double score,
            double lr
    ) {
        return new DifferentialDiagnosisModelImpl(diseaseId, score, lr);
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
