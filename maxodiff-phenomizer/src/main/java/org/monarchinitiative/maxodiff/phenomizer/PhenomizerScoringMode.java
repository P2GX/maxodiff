package org.monarchinitiative.maxodiff.phenomizer;

/**
 * The scoring mode for the Phenomizer algorithm
 */
public enum PhenomizerScoringMode {
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
