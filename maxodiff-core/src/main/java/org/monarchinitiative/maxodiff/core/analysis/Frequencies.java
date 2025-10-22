package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
/**
 * {@code Frequencies} represents the distribution of observed frequencies
 * for a specific Human Phenotype Ontology (HPO) term across a collection
 * of diseases or other entities.
 *
 * <p>This record is typically used to summarize how often a given HPO term
 * appears in association with different diseases, based on annotated data.</p>
 *
 * @param hpoId       the {@link TermId} of the HPO term being summarized
 * @param frequencies list of frequency values (each between 0 and 1) observed
 *                    for this HPO term across diseases or other samples
 */
public record Frequencies(
        TermId hpoId,
        List<Float> frequencies) {
}
