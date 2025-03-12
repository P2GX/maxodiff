package org.monarchinitiative.maxodiff.core.analysis.refinement;

import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementOptionsImpl;

public interface RefinementOptions {

    static RefinementOptions of(int nDiseases, int nRepetitions, double weight) {
        return new RefinementOptionsImpl(nDiseases, nRepetitions, weight);
    }

    int nDiseases();
    int nRepetitions();
    double weight();

}
