package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.HashSet;
import java.util.Set;

public class DiscoverablePhenotypes {

    public static Set<TermId> getDiscoverablePhenotypeIds(Set<TermId> potentialPhenotypes, Set<TermId> excludedPhenotypes) {
        Set<TermId> discoverablePhenotypes = new HashSet<>(potentialPhenotypes);
        excludedPhenotypes.forEach(discoverablePhenotypes::remove);

        return discoverablePhenotypes;
    }
}
