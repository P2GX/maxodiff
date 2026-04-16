package org.p2gx.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

/**
 *
 * @param diseaseId Identifier of a disease
 * @param hpoId Identifier of an HPO term seen in the disease
 * @param frequency Frequency value between 0 and 1, both inclusive
 * @param mica Most informative common ancestor
 */

public record HpoFrequency(
        TermId diseaseId,
        TermId hpoId,
        float frequency,
        float mica) {
}
