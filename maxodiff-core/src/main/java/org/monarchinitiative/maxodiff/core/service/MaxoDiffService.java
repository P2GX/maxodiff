package org.monarchinitiative.maxodiff.core.service;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
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
                              Map<TermId, Set<SimpleTerm>> diseaseToMaxoMap,
                              Map<TermId, Set<SimpleTerm>> diseaseToHpoMap,
                              List<SimpleTerm> allMaxoAnnots,
                              List<SimpleTerm> allHpoAnnots,
                              Map<SimpleTerm, Set<SimpleTerm>> maxoDxAnnots,
                              Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoMap
                              ) {
        return null;//new PhenotypeServiceImpl(hpo, dxmap, diseases);
    }

    Map<TermId, HpoDisease> diseaseMap();
    Map<TermId, Set<SimpleTerm>> diseaseToMaxoMap();
    Map<TermId, Set<SimpleTerm>> diseaseToHpoMap();
    List<SimpleTerm> allMaxoAnnots();
    List<SimpleTerm> allHpoAnnots();
    Map<SimpleTerm, Set<SimpleTerm>> maxoDxAnnots();
    Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoMap();
}
