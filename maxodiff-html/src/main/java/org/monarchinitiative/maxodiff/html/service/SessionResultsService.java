package org.monarchinitiative.maxodiff.html.service;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Deprecated(forRemoval = true)
public class SessionResultsService {

    // TODO: use the Spring configuration or delete the service.

    public SessionResultsService() {}


    public MaxoDiffRefiner getMaxoDiffRefiner(HpoDiseases diseases,
                                              Map<TermId, Set<TermId>> fullHpoToMaxoTermIdMap,
                                              MinimalOntology hpo) {

        return new MaxoDiffRefiner(diseases, fullHpoToMaxoTermIdMap, hpo);
    }


    public Map<String, String> getAllMaxoTermsMap(Map<SimpleTerm, Set<SimpleTerm>> fullHpoToMaxoTermMap) {

        Set<SimpleTerm> allMaxoTerms = fullHpoToMaxoTermMap.values()
                .stream().flatMap(Collection::stream).collect(Collectors.toSet());
        Map<String, String> allMaxoTermsMap = new HashMap<>();
        allMaxoTerms.forEach(st -> allMaxoTermsMap.put(st.tid().toString(), st.label()));

        return allMaxoTermsMap;
    }

    public Map<TermId, String> getAllHpoTermsMap(Map<SimpleTerm, Set<SimpleTerm>> fullHpoToMaxoTermMap) {

        Set<SimpleTerm> allMaxoTerms = fullHpoToMaxoTermMap.keySet();
        Map<TermId, String> allHpoTermsMap = new HashMap<>();
        allMaxoTerms.forEach(st -> allHpoTermsMap.put(st.tid(), st.label()));

        return allHpoTermsMap;
    }

}
