package org.monarchinitiative.maxodiff.core.analysis.refinement;

record RefinementOptionsImpl(
        int nDiseases,
        int nRepetitions,
        double weight
) implements RefinementOptions {
}
