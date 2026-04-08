package org.p2gx.maxodiff.config;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.MySimpleTerm;
import org.p2gx.maxodiff.core.analysis.MySimpleTerm;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;


import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefinerImpl;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.p2gx.maxodiff.core.io.MaxoDxAnnots;
import org.p2gx.maxodiff.core.model.GeneralMaxoTerms;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.p2gx.maxodiff.core.service.BiometadataServiceImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;


public record MaxodiffPropsConfiguration(
        Ontology hpo,
        HpoDiseases hpoDiseases,
        Map<MySimpleTerm, Set<MySimpleTerm>> maxoToHpoTermMap,
        Map<MySimpleTerm, Set<MySimpleTerm>> maxoAnnotsMap,
        BiometadataService biometadataService) {

    public static MaxodiffPropsConfiguration createConfig(MaxodiffDataResolver maxodiffDataResolver) throws IOException {
        MinimalOntology minHpo = MinimalOntologyLoader.loadOntology(maxodiffDataResolver.hpoJson().toFile());
        Ontology hpo = OntologyLoader.loadOntology(maxodiffDataResolver.hpoJson().toFile());
        HpoDiseases diseases = HpoDiseaseLoaders.defaultLoader(minHpo, HpoDiseaseLoaderOptions.defaultOmim()).load(maxodiffDataResolver.phenotypeAnnotations());
        // Map of HPO Term : Set of MAxO Terms
        Map<MySimpleTerm, Set<MySimpleTerm>> maxoAnnotsMap;
        try (BufferedReader reader = Files.newBufferedReader(maxodiffDataResolver.maxoDxAnnots())) {
            maxoAnnotsMap = MaxoDxAnnots.parseHpoToMaxo(reader);
        }
        Map<String, String> generalMaxoTermsMap = GeneralMaxoTerms.getGeneralMaxoTerms();
        Set<MySimpleTerm> generalMaxoTerms = new HashSet<>();
        generalMaxoTermsMap.forEach((key, value) -> generalMaxoTerms.add(new MySimpleTerm(TermId.of(key), value)));
        for (Set<MySimpleTerm> mterms : maxoAnnotsMap.values()) {
            mterms.removeAll(generalMaxoTerms);
        }

        // Map of MAxO : Set of HPO Terms
        Map<MySimpleTerm, Set<MySimpleTerm>> maxoToHpoMap = new HashMap<>();
        for (Map.Entry<MySimpleTerm, Set<MySimpleTerm>> e : maxoAnnotsMap.entrySet()) {
            MySimpleTerm hpoTerm = e.getKey();
            MySimpleTerm hpoIdTerm = new MySimpleTerm(hpoTerm.tid(), hpoTerm.label());
            Set<MySimpleTerm> maxoTerms = e.getValue();
            for (MySimpleTerm maxoTerm : maxoTerms) {
                MySimpleTerm maxoIdTerm = new MySimpleTerm(maxoTerm.tid(), maxoTerm.label());
                if (!maxoToHpoMap.containsKey(maxoIdTerm)) {
                    maxoToHpoMap.put(maxoIdTerm, new HashSet<>(Collections.singleton(hpoIdTerm)));
                } else {
                    Set<MySimpleTerm> hpoIdTerms = maxoToHpoMap.get(maxoIdTerm);
                    hpoIdTerms.add(hpoIdTerm);
                    maxoToHpoMap.replace(maxoIdTerm, hpoIdTerms);
                }
            }
        }

        BiometadataService biometadataService = BiometadataServiceImpl.of(minHpo, diseases, maxoAnnotsMap);
        return new MaxodiffPropsConfiguration(hpo, diseases, maxoToHpoMap, maxoAnnotsMap, biometadataService);
    }

    public DiffDiagRefiner diffDiagRefiner() {

        return new DiffDiagRefinerImpl(hpoDiseases, maxoToHpoTermMap, maxoAnnotsMap, hpo);

    }
}
