package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.HashSet;
import java.util.Set;

/**
 * This class calculates the discoverable phenotypes, i.e. potential phenotypes not including assumed
 * excluded phenotypes.
 */
public class DiscoverablePhenotypes {

    /**
     *
     * @param potentialPhenotypes Set of potential phenotypes calculated with {@link AscertainablePhenotypes#getAscertainablePhenotypeIds} method.
     * @param excludedPhenotypes Set of excluded phenotypes calculated with {@link ExcludedPhenotypes#getExcludedPhenotypes} method.
     * @return Set of discoverable phenotypes, i.e. potential phenotypes not including assumed excluded phenotypes.
     */
    public static Set<TermId> getDiscoverablePhenotypeIds(Set<TermId> potentialPhenotypes, Set<TermId> excludedPhenotypes) {
        Set<TermId> discoverablePhenotypes = new HashSet<>(potentialPhenotypes);
        excludedPhenotypes.forEach(discoverablePhenotypes::remove);

        return discoverablePhenotypes;
    }
}
