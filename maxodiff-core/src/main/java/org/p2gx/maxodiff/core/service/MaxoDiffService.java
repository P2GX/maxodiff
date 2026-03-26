package org.p2gx.maxodiff.core.service;

import org.p2gx.maxodiff.core.SimpleTermOld;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Set;


/*
private final ;

private final Ontology hpo;

private final Map<SimpleTerm, Set<SimpleTerm>> maxoDxAnnots;

private final ;
private final;

private final ;
private final ;
 */
public interface MaxoDiffService {

    static MaxoDiffService of(Map<TermId, HpoDisease> diseaseMap,
                              Map<TermId, Set<SimpleTermOld>> diseaseToMaxoMap,
                              Map<TermId, Set<SimpleTermOld>> diseaseToHpoMap,
                              List<SimpleTermOld> allMaxoAnnots,
                              List<SimpleTermOld> allHpoAnnots,
                              Map<SimpleTermOld, Set<SimpleTermOld>> maxoDxAnnots,
                              Map<SimpleTermOld, Set<SimpleTermOld>> maxoToHpoMap
                              ) {
        return null;//new PhenotypeServiceImpl(hpo, dxmap, diseases);
    }

    Map<TermId, HpoDisease> diseaseMap();
    Map<TermId, Set<SimpleTermOld>> diseaseToMaxoMap();
    Map<TermId, Set<SimpleTermOld>> diseaseToHpoMap();
    List<SimpleTermOld> allMaxoAnnots();
    List<SimpleTermOld> allHpoAnnots();
    Map<SimpleTermOld, Set<SimpleTermOld>> maxoDxAnnots();
    Map<SimpleTermOld, Set<SimpleTermOld>> maxoToHpoMap();
}
