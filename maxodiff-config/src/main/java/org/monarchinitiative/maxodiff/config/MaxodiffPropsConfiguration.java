package org.monarchinitiative.maxodiff.config;

import org.monarchinitiative.maxodiff.core.SimpleTermOld;
import org.monarchinitiative.maxodiff.core.analysis.refinement.*;
import org.monarchinitiative.maxodiff.core.io.MaxoDxAnnots;
import org.monarchinitiative.maxodiff.core.model.GeneralMaxoTerms;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.core.service.BiometadataServiceImpl;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;


public record MaxodiffPropsConfiguration(
        Ontology hpo,
        HpoDiseases hpoDiseases,
        Map<SimpleTermOld, Set<SimpleTermOld>> maxoAnnotsMap,
        BiometadataService biometadataService) {

    public static MaxodiffPropsConfiguration createConfig(MaxodiffDataResolver maxodiffDataResolver) throws IOException {
        MinimalOntology minHpo = MinimalOntologyLoader.loadOntology(maxodiffDataResolver.hpoJson().toFile());
        Ontology hpo = OntologyLoader.loadOntology(maxodiffDataResolver.hpoJson().toFile());
        HpoDiseases diseases = HpoDiseaseLoaders.defaultLoader(minHpo, HpoDiseaseLoaderOptions.defaultOmim()).load(maxodiffDataResolver.phenotypeAnnotations());
        // Map of HPO Id : Set of MAxO Ids
        Map<SimpleTermOld, Set<SimpleTermOld>> maxoAnnotsMap;
        try (BufferedReader reader = Files.newBufferedReader(maxodiffDataResolver.maxoDxAnnots())) {
            maxoAnnotsMap = MaxoDxAnnots.parseHpoToMaxo(reader);
        }
        Map<TermId, String> generalMaxoTermsMap = GeneralMaxoTerms.getGeneralMaxoTerms();
        Set<SimpleTermOld> generalMaxoTerms = new HashSet<>();
        generalMaxoTermsMap.entrySet().forEach(entry ->
                generalMaxoTerms.add(new SimpleTermOld(entry.getKey(), entry.getValue())));
        for (Set<SimpleTermOld> mterms : maxoAnnotsMap.values()) {
            mterms.removeAll(generalMaxoTerms);
        }

        BiometadataService biometadataService = BiometadataServiceImpl.of(minHpo, diseases, maxoAnnotsMap);
        return new MaxodiffPropsConfiguration(hpo, diseases, maxoAnnotsMap, biometadataService);
    }

    public DiffDiagRefiner diffDiagRefiner() {
        Map<TermId, Set<TermId>> hpoToMaxoIdMap = new HashMap<>();
        for (Map.Entry<SimpleTermOld, Set<SimpleTermOld>> entry : maxoAnnotsMap.entrySet()) {
            TermId hpoId = entry.getKey().tid();
            Set<TermId> maxoIds = new HashSet<>();
            maxoAnnotsMap.get(entry.getKey()).forEach(t -> maxoIds.add(t.tid()));
            hpoToMaxoIdMap.put(hpoId, maxoIds);
        }
        return new BaseDiffDiagRefiner(hpoDiseases, hpoToMaxoIdMap, maxoAnnotsMap, hpo);

    }
}
