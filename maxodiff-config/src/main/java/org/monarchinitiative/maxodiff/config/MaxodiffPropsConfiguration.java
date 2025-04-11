package org.monarchinitiative.maxodiff.config;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.refinement.*;
import org.monarchinitiative.maxodiff.core.io.MaxoDxAnnots;
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


public record MaxodiffPropsConfiguration(MinimalOntology minHpo, Ontology hpo, HpoDiseases hpoDiseases,
                                         Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap,
                                         BiometadataService biometadataService) {

    public static MaxodiffPropsConfiguration createConfig(MaxodiffDataResolver maxodiffDataResolver) throws IOException {
        MinimalOntology minHpo = MinimalOntologyLoader.loadOntology(maxodiffDataResolver.hpoJson().toFile());
        Ontology hpo = OntologyLoader.loadOntology(maxodiffDataResolver.hpoJson().toFile());
        HpoDiseases diseases = HpoDiseaseLoaders.defaultLoader(minHpo, HpoDiseaseLoaderOptions.defaultOptions()).load(maxodiffDataResolver.phenotypeAnnotations());
        Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap;
        try (BufferedReader reader = Files.newBufferedReader(maxodiffDataResolver.maxoDxAnnots())) {
            maxoAnnotsMap = MaxoDxAnnots.parseHpoToMaxo(reader);
        }
        BiometadataService biometadataService = BiometadataServiceImpl.of(minHpo, diseases, maxoAnnotsMap);
        return new MaxodiffPropsConfiguration(minHpo, hpo, diseases, maxoAnnotsMap, biometadataService);
    }

    public DiffDiagRefiner diffDiagRefiner(String refiner) {

        Map<TermId, Set<TermId>> hpoToMaxoIdMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : maxoAnnotsMap.entrySet()) {
            TermId hpoId = entry.getKey().tid();
            Set<TermId> maxoIds = new HashSet<>();
            maxoAnnotsMap.get(entry.getKey()).forEach(t -> maxoIds.add(t.tid()));
            hpoToMaxoIdMap.put(hpoId, maxoIds);
        }
//        if (dummy) {
//            return new DummyDiffDiagRefiner(hpoDiseases, hpoToMaxoIdMap, hpo);
//        } else {
//            return new MaxoDiffRefiner(hpoDiseases, hpoToMaxoIdMap, hpo);
//        }
        DiffDiagRefiner diffDiagRefiner = null;
        switch (refiner) {
            case "score" -> diffDiagRefiner = new MaxoDiffRefiner(hpoDiseases, hpoToMaxoIdMap, maxoAnnotsMap, minHpo, hpo);
            case "dummy" -> diffDiagRefiner = new DummyDiffDiagRefiner(hpoDiseases, hpoToMaxoIdMap, maxoAnnotsMap, minHpo, hpo);
            case "rank" -> diffDiagRefiner = new MaxoDiffRankRefiner(hpoDiseases, hpoToMaxoIdMap, maxoAnnotsMap, minHpo, hpo);
            case "ddScore" -> diffDiagRefiner = new MaxoDiffDDScoreRefiner(hpoDiseases, hpoToMaxoIdMap, maxoAnnotsMap, minHpo, hpo);
            case "ksTest" -> diffDiagRefiner = new MaxoDiffKolmogorovSmirnovRefiner(hpoDiseases, hpoToMaxoIdMap, maxoAnnotsMap, minHpo, hpo);
        }
        return diffDiagRefiner;
    }
}
