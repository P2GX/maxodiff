package org.monarchinitiative.maxodiff.phenomizer;

public enum ScoringMode {
    /**
     * Compute one-sided similarity of a query (individual) to a disease.
     */
    ONE_SIDED,

    /**
     * Compute a two-sided similarity between a query (individual) and a disease.
     * <p>
     * This involves computing mean of one-sided similarities between query and disease.
     */
    TWO_SIDED,
}
