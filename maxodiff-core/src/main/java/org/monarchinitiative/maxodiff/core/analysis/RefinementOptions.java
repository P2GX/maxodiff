package org.monarchinitiative.maxodiff.core.analysis;

public interface RefinementOptions {

    static RefinementOptions of(int nDiseases, double weight) {
        return new RefinementOptionsImpl(nDiseases, weight);
    }

    int nDiseases();
    double weight();

}
