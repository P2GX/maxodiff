package org.monarchinitiative.maxodiff.core.analysis;

/**
 *
 * @param omimId Identifier of a disease
 * @param hpoId Identifier of an HPO term seen in the disease
 * @param frequency Frequency value between 0 and 1, both inclusive.
 */

public record HpoFrequency(
        String omimId,
        String hpoId,
        float frequency,
        float mica) {
}
