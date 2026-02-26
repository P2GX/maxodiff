package org.monarchinitiative.maxodiff.core.analysis.refinement;

public record RefinementOptions (
        int nDiseases,
        int nRepetitions) {

    static public RefinementOptions of(int nDiseases, int nRepetitions) {
        return new RefinementOptions(nDiseases, nRepetitions);
    }


}
