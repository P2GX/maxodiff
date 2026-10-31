package org.p2gx.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Objects;

/**
 * An HPO term and how strongly the top candidate diseases support observing it.
 * Weights are relative: they are not required to sum to 1.
 *
 * @param hpoId  the HPO term
 * @param weight a positive, finite relative weight
 */
public record HpoWeight(TermId hpoId, double weight) {

    public HpoWeight {
        Objects.requireNonNull(hpoId, "hpoId must not be null");
        if (!(weight > 0) || Double.isInfinite(weight)) { // also rejects NaN
            throw new IllegalArgumentException("weight must be positive and finite but was " + weight);
        }
    }
}