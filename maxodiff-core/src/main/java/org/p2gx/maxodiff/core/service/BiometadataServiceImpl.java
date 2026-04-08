package org.p2gx.maxodiff.core.service;

import org.p2gx.maxodiff.core.analysis.MySimpleTerm;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;
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
    private final MinimalOntology ontology;
    private final Map<TermId, String> diseaseTermsMap;

    public static BiometadataServiceImpl of(MinimalOntology hpo, HpoDiseases hpoDiseases, Map<MySimpleTerm, Set<MySimpleTerm>> maxoAnnotsMap) {
        Map<TermId, String> diseaseToLabel = hpoDiseases.hpoDiseases().collect(Collectors.toMap(HpoDisease::id, HpoDisease::diseaseName));
        // Note, we assume that there are no MAxO terms with identical ids but different labels.
        Map<String, String> maxoTermsMap = maxoAnnotsMap.values().stream()
                .flatMap(Collection::stream).distinct()
                .collect(Collectors.toMap(mst -> String.valueOf(mst.tid()), MySimpleTerm::label));
        return new BiometadataServiceImpl(maxoTermsMap, hpo, diseaseToLabel);
    }

    public BiometadataServiceImpl(Map<String, String> maxoTermsMap,
                                  MinimalOntology hpo,
                                  Map<TermId, String> diseaseTermsMap) {

        this.maxoTermsMap = maxoTermsMap;
        this.ontology = hpo;
        this.diseaseTermsMap = diseaseTermsMap;
    }


    @Override
    public Optional<String> hpoLabel(TermId termId) {
        Optional<Term> opt = this.ontology.termForTermId(termId);
        return opt.map(Term::getName);
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
