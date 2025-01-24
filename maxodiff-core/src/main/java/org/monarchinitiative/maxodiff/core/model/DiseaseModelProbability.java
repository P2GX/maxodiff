package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.maxodiff.core.analysis.impl.DiseaseExponentialDecayProbabilityImpl;
import org.monarchinitiative.maxodiff.core.analysis.impl.DiseaseRankedProbabilityImpl;
import org.monarchinitiative.maxodiff.core.analysis.impl.DiseaseSoftmaxProbabilityImpl;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

public interface DiseaseModelProbability {

    double DEFAULT_LAMBDA = 1.;

    double probability(TermId targetDiseaseId);

    static DiseaseModelProbability ranked(List<DifferentialDiagnosis> diagnoses) {
        return new DiseaseRankedProbabilityImpl(diagnoses);
    }

    static DiseaseModelProbability softmax(List<DifferentialDiagnosis> diagnoses) {
        return new DiseaseSoftmaxProbabilityImpl(diagnoses);
    }

    static DiseaseModelProbability exponentialDecay(List<DifferentialDiagnosis> diagnoses) {
        return new DiseaseExponentialDecayProbabilityImpl(diagnoses, DEFAULT_LAMBDA);
    }

    static DiseaseModelProbability exponentialDecay(List<DifferentialDiagnosis> diagnoses, double lambda) {
        return new DiseaseExponentialDecayProbabilityImpl(diagnoses, lambda);
    }

}
