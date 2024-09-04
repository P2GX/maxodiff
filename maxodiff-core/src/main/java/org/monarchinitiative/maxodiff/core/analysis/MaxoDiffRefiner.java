package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class MaxoDiffRefiner extends BaseDiffDiagRefiner {

    public MaxoDiffRefiner(HpoDiseases hpoDiseases, Map<TermId, Set<TermId>> fullHpoToMaxoTermMap,
                           MinimalOntology hpo) {
        super(hpoDiseases, fullHpoToMaxoTermMap, hpo);
    }

}
