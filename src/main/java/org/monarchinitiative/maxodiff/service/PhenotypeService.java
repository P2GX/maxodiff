package org.monarchinitiative.maxodiff.service;

import org.monarchinitiative.maxodiff.model.SimpleTerm;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;

import java.util.Map;
import java.util.Set;

public interface PhenotypeService {

    static PhenotypeService of(Ontology hpo,
                               Map<SimpleTerm, Set<SimpleTerm>> dxmap,
                               HpoDiseases diseases) {
        return new PhenotypeServiceImpl(hpo, dxmap, diseases);
    }

    Ontology hpo();

    Map<SimpleTerm, Set<SimpleTerm>> maxoDxAnnots();

    HpoDiseases diseases();



}
