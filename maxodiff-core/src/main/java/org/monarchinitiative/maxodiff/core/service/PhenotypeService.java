package org.monarchinitiative.maxodiff.core.service;

import org.monarchinitiative.maxodiff.core.SimpleTermOld;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;

import java.util.Map;
import java.util.Set;

public interface PhenotypeService {

    static PhenotypeService of(Ontology hpo,
                               Map<SimpleTermOld, Set<SimpleTermOld>> dxmap,
                               HpoDiseases diseases) {
        return new PhenotypeServiceImpl(hpo, dxmap, diseases);
    }

    Ontology hpo();

    Map<SimpleTermOld, Set<SimpleTermOld>> maxoDxAnnots();

    HpoDiseases diseases();



}
