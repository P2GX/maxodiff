package org.p2gx.maxodiff.core.model;

import org.p2gx.maxodiff.core.analysis.impl.DiseaseRankedProbabilityImpl;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

public interface DiseaseModelProbability {

    double DEFAULT_LAMBDA = 1.;

    double probability(TermId targetDiseaseId);

    static DiseaseModelProbability ranked(List<DifferentialDiagnosis> diagnoses) {
        return new DiseaseRankedProbabilityImpl(diagnoses);
    }

}
