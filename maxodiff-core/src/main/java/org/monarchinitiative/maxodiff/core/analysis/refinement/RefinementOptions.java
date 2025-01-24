package org.monarchinitiative.maxodiff.core.analysis.refinement;

import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementOptionsImpl;

public interface RefinementOptions {

    static RefinementOptions of(int nDiseases, double weight) {
        return new RefinementOptionsImpl(nDiseases, weight);
    }

    int nDiseases();
    double weight();

}
