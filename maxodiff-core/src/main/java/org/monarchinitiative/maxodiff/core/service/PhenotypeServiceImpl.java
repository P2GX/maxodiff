package org.monarchinitiative.maxodiff.core.service;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;

import java.util.Map;
import java.util.Set;

public record PhenotypeServiceImpl(Ontology hpo,
                                   Map<SimpleTerm, Set<SimpleTerm>> maxoDxAnnots,
                                   HpoDiseases diseases) implements PhenotypeService {
}

