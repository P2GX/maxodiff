package org.monarchinitiative.maxodiff.core.analysis.refinement;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class MaxoDiffRefiner extends BaseDiffDiagRefiner {

    public MaxoDiffRefiner(HpoDiseases hpoDiseases, Map<TermId, Set<TermId>> fullHpoToMaxoTermIdMap,
                           Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap, MinimalOntology hpo) {
        super(hpoDiseases, fullHpoToMaxoTermIdMap, hpoToMaxoTermMap, hpo);
    }

}
