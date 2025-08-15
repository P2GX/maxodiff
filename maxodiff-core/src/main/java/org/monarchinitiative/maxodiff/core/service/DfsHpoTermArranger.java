package org.monarchinitiative.maxodiff.core.service;

import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.Dbxref;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class DfsHpoTermArranger implements HpoTermArranger{

    private static final TermId PHENOTYPIC_ABNORMALITY = TermId.of("HP:0000118");

    private final Ontology ontology;

    private Set<TermId> termsForInclusion = new HashSet<>();


    public DfsHpoTermArranger(Ontology ontology) {
        this.ontology  = ontology;
    }

    @Override
    public List<TermId> arrangeTerms(List<TermId> termIds) {
        termsForInclusion.clear();
        termsForInclusion.addAll(termIds);
        Set<TermId> visited = new HashSet<>();
        List<TermId> orderedList = new ArrayList<>();
        dfs(PHENOTYPIC_ABNORMALITY, visited, orderedList);
        return orderedList;
    }

    /**
     * Perform a depth-first search to arrange the terms for curation into an order that
     * tends to keep related terms together
     * We only store the terms we are interested in ordered_tids.
     * @param tid
     * @param visited
     * @param result
     */
    private void dfs(TermId tid, Set<TermId> visited, List<TermId> result) {
        if (visited.contains(tid)) {
            return;
        }
        visited.add(tid);
        if (! tid.equals(PHENOTYPIC_ABNORMALITY) && !this.ontology.graph().existsPath(tid, PHENOTYPIC_ABNORMALITY)) {
            // do not consider terms that are outside of the Phenotype subontology
            return;
        }
        if (this.termsForInclusion.contains(tid)) {
            result.add(tid);
        }
        for (TermId childTid : this.ontology.graph().getChildren(tid)) {
            dfs(childTid, visited, result);
        }
    }
}
