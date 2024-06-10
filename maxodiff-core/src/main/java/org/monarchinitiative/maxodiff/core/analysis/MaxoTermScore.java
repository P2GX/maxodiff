package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Set;

public record MaxoTermScore(String maxoId, Integer nOmimTerms, Set<TermId> omimTermIds,
                            Integer nHpoTerms, Set<TermId> hpoTermIds,
                            Double initialScore, Double score, Double scoreDiff) {
}
