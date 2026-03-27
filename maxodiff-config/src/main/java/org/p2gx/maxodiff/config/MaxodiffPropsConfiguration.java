package org.p2gx.maxodiff.config;

import org.p2gx.maxodiff.core.analysis.SimpleTerm;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;


import org.p2gx.maxodiff.core.analysis.refinement.BaseDiffDiagRefiner;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.p2gx.maxodiff.core.io.MaxoDxAnnots;
import org.p2gx.maxodiff.core.model.GeneralMaxoTerms;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.p2gx.maxodiff.core.service.BiometadataServiceImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;


public record MaxodiffPropsConfiguration(
        Ontology hpo,
        HpoDiseases hpoDiseases,
        Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap,
        BiometadataService biometadataService) {

    public static MaxodiffPropsConfiguration createConfig(MaxodiffDataResolver maxodiffDataResolver) throws IOException {
        MinimalOntology minHpo = MinimalOntologyLoader.loadOntology(maxodiffDataResolver.hpoJson().toFile());
        Ontology hpo = OntologyLoader.loadOntology(maxodiffDataResolver.hpoJson().toFile());
        HpoDiseases diseases = HpoDiseaseLoaders.defaultLoader(minHpo, HpoDiseaseLoaderOptions.defaultOmim()).load(maxodiffDataResolver.phenotypeAnnotations());
        // Map of HPO Id : Set of MAxO Ids
        Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap;
        try (BufferedReader reader = Files.newBufferedReader(maxodiffDataResolver.maxoDxAnnots())) {
            maxoAnnotsMap = MaxoDxAnnots.parseHpoToMaxo(reader);
        }
        Map<String, String> generalMaxoTermsMap = GeneralMaxoTerms.getGeneralMaxoTerms();
        Set<SimpleTerm> generalMaxoTerms = new HashSet<>();
        generalMaxoTermsMap.forEach((key, value) -> generalMaxoTerms.add(new SimpleTerm(key, value)));
        for (Set<SimpleTerm> mterms : maxoAnnotsMap.values()) {
            mterms.removeAll(generalMaxoTerms);
        }

        BiometadataService biometadataService = BiometadataServiceImpl.of(minHpo, diseases, maxoAnnotsMap);
        return new MaxodiffPropsConfiguration(hpo, diseases, maxoAnnotsMap, biometadataService);
    }

    public DiffDiagRefiner diffDiagRefiner() {
        Map<String, Set<String>> hpoToMaxoIdMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : maxoAnnotsMap.entrySet()) {
            String hpoId = entry.getKey().termId();
            Set<String> maxoIds = new HashSet<>();
            maxoAnnotsMap.get(entry.getKey()).forEach(t -> maxoIds.add(t.termId()));
            hpoToMaxoIdMap.put(hpoId, maxoIds);
        }
        return new BaseDiffDiagRefiner(hpoDiseases, hpoToMaxoIdMap, maxoAnnotsMap, hpo);

    }
}
