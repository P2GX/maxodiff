package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;
import java.util.Set;

public record MaxoTermScore(String maxoId, String maxoLabel, Integer nOmimTerms, Set<TermId> omimTermIds,
                            Integer nHpoTerms, Set<TermId> hpoTermIds, Map<TermId, Double> probabilityMap,
                            Double initialScore, Double score, Double scoreDiff) {
}
