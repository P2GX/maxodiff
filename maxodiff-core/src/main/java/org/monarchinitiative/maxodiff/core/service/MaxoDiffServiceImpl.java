package org.monarchinitiative.maxodiff.core.service;

import org.monarchinitiative.maxodiff.core.SimpleTermOld;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record MaxoDiffServiceImpl(Map<TermId, HpoDisease> diseaseMap,
                                  Map<TermId, Set<SimpleTermOld>> diseaseToMaxoMap,
                                  Map<TermId, Set<SimpleTermOld>> diseaseToHpoMap,
                                  List<SimpleTermOld> allMaxoAnnots,
                                  List<SimpleTermOld> allHpoAnnots,
                                  Map<SimpleTermOld, Set<SimpleTermOld>> maxoDxAnnots,
                                  Map<SimpleTermOld, Set<SimpleTermOld>> maxoToHpoMap) implements  MaxoDiffService {
}
