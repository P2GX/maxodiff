package org.monarchinitiative.maxodiff.html.config;

import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.io.MaxoDxAnnots;
import org.monarchinitiative.maxodiff.config.MaxodiffDataException;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.DiffDiagRefiner;
import org.monarchinitiative.maxodiff.core.analysis.MaxoDiffRefiner;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.core.service.BiometadataServiceImpl;
import org.monarchinitiative.maxodiff.html.service.DifferentialDiagnosisEngineService;
import org.monarchinitiative.maxodiff.html.service.DifferentialDiagnosisEngineServiceImpl;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Configuration
@EnableConfigurationProperties({MaxodiffProperties.class})
public class MaxodiffAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaxodiffAutoConfiguration.class);

    @Bean
    public Path maxodiffDataDirectory(MaxodiffProperties maxodiffProperties) throws MaxodiffDataException {
        if (maxodiffProperties.getDataDirectory() == null) {
            throw new MaxodiffDataException("Maxodiff data directory was not provided");
        }
        Path dataDirectory = Path.of(maxodiffProperties.getDataDirectory());
        if (!Files.isDirectory(dataDirectory)) {
            throw new MaxodiffDataException("%s is not a directory".formatted(dataDirectory.toAbsolutePath()));
        }
        LOGGER.info("Loading Maxodiff resources from {}", dataDirectory.toAbsolutePath());
        return dataDirectory;
    }

    @Bean
    public MaxodiffDataResolver maxodiffDataResolver(Path maxodiffDataDirectory) throws MaxodiffDataException {
        return MaxodiffDataResolver.of(maxodiffDataDirectory);
    }

    @Bean
    public MinimalOntology hpo(MaxodiffDataResolver maxodiffDataResolver) {
        LOGGER.debug("Loading HPO JSON from {}", maxodiffDataResolver.hpoJson().toAbsolutePath());
        return MinimalOntologyLoader.loadOntology(maxodiffDataResolver.hpoJson().toFile());
    }

    @Bean
    public HpoDiseases hpoDiseases(MinimalOntology hpo, MaxodiffDataResolver maxodiffDataResolver) throws IOException {
        LOGGER.debug("Loading HPO annotations from {}", maxodiffDataResolver.phenotypeAnnotations().toAbsolutePath());
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(hpo, HpoDiseaseLoaderOptions.defaultOptions());
        return loader.load(maxodiffDataResolver.phenotypeAnnotations());
    }

    @Bean
    public Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap(MaxodiffDataResolver maxodiffDataResolver) throws IOException {
        LOGGER.debug("Loading MAxO annotations from {}", maxodiffDataResolver.maxoDxAnnots().toAbsolutePath());
        try (BufferedReader reader = Files.newBufferedReader(maxodiffDataResolver.maxoDxAnnots())) {
            return MaxoDxAnnots.parseHpoToMaxo(reader);
        }
    }

    @Bean
    public Map<TermId, Set<TermId>> hpoToMaxoIdMap(Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap) {
        Map<TermId, Set<TermId>> hpoToMaxoIdMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : maxoAnnotsMap.entrySet()) {
            TermId hpoId = entry.getKey().tid();
            Set<TermId> maxoIds = new HashSet<>();
            maxoAnnotsMap.get(entry.getKey()).forEach(t -> maxoIds.add(t.tid()));
            hpoToMaxoIdMap.put(hpoId, maxoIds);
        }
        return hpoToMaxoIdMap;
    }

    @Bean
    public BiometadataService biometadataService(
            MinimalOntology hpo,
            HpoDiseases hpoDiseases,
            Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap) {
        return BiometadataServiceImpl.of(hpo, hpoDiseases, maxoAnnotsMap);
    }

    @Bean
    public DiffDiagRefiner diffDiagRefiner(
            MinimalOntology hpo,
            HpoDiseases hpoDiseases,
            Map<TermId, Set<TermId>> hpoToMaxoIdMap,
            Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap) {

        return new MaxoDiffRefiner(hpoDiseases, hpoToMaxoIdMap, maxoAnnotsMap, hpo);
    }

    @Bean
    public DifferentialDiagnosisEngineService differentialDiagnosisEngineService() {
        // TODO: make LIRICAL and Exomiser differential diagnosis engine
        Map<String, DifferentialDiagnosisEngine> engines = Map.of();
        return DifferentialDiagnosisEngineServiceImpl.of(engines);
    }
}
