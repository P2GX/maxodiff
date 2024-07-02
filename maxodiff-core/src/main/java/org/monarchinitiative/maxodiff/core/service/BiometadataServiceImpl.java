package org.monarchinitiative.maxodiff.core.service;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class BiometadataServiceImpl implements BiometadataService {

    private final Map<String, String> maxoTermsMap;
    private final Map<TermId, String> hpoTermsMap;
    private final Map<TermId, String> diseaseTermsMap;

    public static BiometadataServiceImpl of(MinimalOntology hpo, HpoDiseases hpoDiseases, Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap) {
        Map<TermId, String> hpoToLabel = hpo.getTerms().stream().collect(Collectors.toMap(Term::id, Term::getName));
        Map<TermId, String> diseaseToLabel = hpoDiseases.hpoDiseases().collect(Collectors.toMap(HpoDisease::id, HpoDisease::diseaseName));
        // Note, we assume that there are no MAxO terms with identical ids but different labels.
        Map<String, String> maxoTermsMap = maxoAnnotsMap.values().stream()
                .flatMap(Collection::stream).distinct()
                .collect(Collectors.toMap(t -> t.tid().getValue(), SimpleTerm::label));
        return new BiometadataServiceImpl(maxoTermsMap, hpoToLabel, diseaseToLabel);
    }

    public BiometadataServiceImpl(Map<String, String> maxoTermsMap,
                                  Map<TermId, String> hpoTermsMap,
                                  Map<TermId, String> diseaseTermsMap) {

        this.maxoTermsMap = maxoTermsMap;
        this.hpoTermsMap = hpoTermsMap;
        this.diseaseTermsMap = diseaseTermsMap;
    }


    @Override
    public Optional<String> hpoLabel(TermId termId) {
        return Optional.ofNullable(hpoTermsMap.get(termId));
    }

    @Override
    public Optional<String> maxoLabel(String curie) {
        return Optional.ofNullable(maxoTermsMap.get(curie));
    }

    @Override
    public Optional<String> diseaseLabel(TermId diseaseId) {
        return Optional.ofNullable(diseaseTermsMap.get(diseaseId));
    }
}
