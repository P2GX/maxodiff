package org.monarchinitiative.maxodiff.core.service;

import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;

public interface HpoTermArranger {

    List<TermId> arrangeTerms(List<TermId> termIds);


    static HpoTermArranger dfs(Ontology ontology) {
        return new DfsHpoTermArranger(ontology);
    }

}
