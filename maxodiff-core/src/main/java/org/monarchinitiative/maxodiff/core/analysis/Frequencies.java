package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

public record Frequencies(TermId hpoId, List<Float> frequencies) {
}
