package org.monarchinitiative.maxodiff.core.analysis;

/**
 *
 * @param omimId
 * @param hpoId
 * @param count
 * @param frequency Frequency value between 0 and 1, both inclusive. It is nullable.
 */

public record HpoFrequency(String omimId, String hpoId, int count, Float frequency) {
}
