package org.p2gx.maxodiff.core.mica;


import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A static utility class for computing IC<sub>MICA</sub> values for term pairs.
 */
public class HpoResnikSimilarityPrecomputer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HpoResnikSimilarityPrecomputer.class);

    private HpoResnikSimilarityPrecomputer() {
    }

    /** Compute symmetrix, pairwise similarities for all pairs of terms in "similarities"; these will be all the terms
     * in an HPO Top-Level Category (Organ)
     */
    private static void computePairwiseSimilarities(
            List<TermId> terms,
            MinimalOntology hpo,
            Map<TermId, Double> termToIc,
            Map<TermPair, Double> similarities
    ) {
        int n = terms.size();

        for (int i = 0; i < n; i++) {
            TermId a = terms.get(i);

            for (int j = i; j < n; j++) {
                TermId b = terms.get(j);

                similarities.merge(
                        TermPair.symmetric(a, b),
                        computeResnikSimilarity(a, b, termToIc, hpo),
                        Math::max
                );
            }
        }
    }

    public static Map<TermPair, Double> precomputeSimilaritiesForTermPairs(
            MinimalOntology hpo,
            Map<TermId, Double> termToIc
    ) {
        Map<TermPair, Double> similarities = new HashMap<>();
        List<TermId> topLevelTerms = toplevelTerms(hpo);
        ProgressBar progress = new ProgressBar("Calculating MICA", topLevelTerms.size());

        for (TermId root : topLevelTerms) {
            List<TermId> descendants = hpo.graph()
                    .getDescendantsStream(root)
                    .distinct()
                    .toList();

            hpo.termForTermId(root).ifPresent(term ->
                    LOGGER.trace(
                            "Working on {} [{}]: {} descendants",
                            term.getName(),
                            term.id().getValue(),
                            descendants.size()
                    )
            );

            computePairwiseSimilarities(descendants, hpo, termToIc, similarities);
            progress.step();
        }

        return similarities;
    }


    /**
     * Compute similarity as the information content of the Most Informative Common Ancestor (MICA)
     * @param a The first TermId
     * @param b The second TermId
     * @param termToIc Map from TermId to information content of the term
     * @param ontology Here, a subontology of the HPO
     * @return the Resnik similarity
     */
    private static double computeResnikSimilarity(TermId a, TermId b,
                                                  Map<TermId, Double> termToIc,
                                                  MinimalOntology ontology) {
        Set<TermId> aAnc = ontology.graph().getAncestorSet(a);
        aAnc.add(a);
        Set<TermId> bAnc = ontology.graph().getAncestorSet(b);
        bAnc.add(b);
        aAnc.retainAll(bAnc);
        return aAnc.stream()
                .map(termToIc::get)
                .filter(Objects::nonNull)
                .reduce(0., Double::max);
    }

    /**
     * List of top level terms that with a few rare exceptions which we will ignore, do
     * not have multiple parentage relations with each other.
     * @return list of top-level HPO terms (i.e., children of Phenotypic abnormality)
     */
    private static List<TermId> toplevelTerms(MinimalOntology hpo) {
        TermId phenotypicAbnormality = TermId.of("HP:0000118");
        Set<TermId> topLevelSet = hpo.graph().getChildren(phenotypicAbnormality);
        return new ArrayList<>(topLevelSet);
    }
}