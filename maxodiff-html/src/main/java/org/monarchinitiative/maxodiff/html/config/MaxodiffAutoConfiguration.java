package org.monarchinitiative.maxodiff.html.config;

import org.monarchinitiative.maxodiff.core.analysis.refinement.BaseDiffDiagRefiner;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.io.MaxoDxAnnots;
import org.monarchinitiative.maxodiff.config.MaxodiffDataException;
import org.monarchinitiative.maxodiff.core.SimpleTermOld;
import org.monarchinitiative.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.core.model.GeneralMaxoTerms;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.core.service.BiometadataServiceImpl;
import org.monarchinitiative.maxodiff.html.service.DifferentialDiagnosisEngineService;
import org.monarchinitiative.maxodiff.html.service.DifferentialDiagnosisEngineServiceImpl;
import org.monarchinitiative.maxodiff.phenomizer.IcMicaData;
import org.monarchinitiative.maxodiff.phenomizer.IcMicaDictLoader;
import org.monarchinitiative.maxodiff.phenomizer.IcMicaDictMetadata;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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
    public MinimalOntology minHpo(MaxodiffDataResolver maxodiffDataResolver) {
        LOGGER.debug("Loading HPO JSON from {}", maxodiffDataResolver.hpoJson().toAbsolutePath());
        return MinimalOntologyLoader.loadOntology(maxodiffDataResolver.hpoJson().toFile());
    }

    @Bean
    public Ontology hpo(MaxodiffDataResolver maxodiffDataResolver) {
        LOGGER.debug("Loading HPO JSON from {}", maxodiffDataResolver.hpoJson().toAbsolutePath());
        return OntologyLoader.loadOntology(maxodiffDataResolver.hpoJson().toFile());
    }

    @Bean
    public HpoDiseases hpoDiseases(MinimalOntology minHpo, MaxodiffDataResolver maxodiffDataResolver) throws IOException {
        LOGGER.debug("Loading HPO annotations from {}", maxodiffDataResolver.phenotypeAnnotations().toAbsolutePath());
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(minHpo, HpoDiseaseLoaderOptions.defaultOmim());
        return loader.load(maxodiffDataResolver.phenotypeAnnotations());
    }

    @Bean
    public Map<SimpleTermOld, Set<SimpleTermOld>> maxoAnnotsMap(MaxodiffDataResolver maxodiffDataResolver) throws IOException {
        LOGGER.debug("Loading MAxO annotations from {}", maxodiffDataResolver.maxoDxAnnots().toAbsolutePath());
        try (BufferedReader reader = Files.newBufferedReader(maxodiffDataResolver.maxoDxAnnots())) {
            Map<SimpleTermOld, Set<SimpleTermOld>> maxoAnnotsMap = MaxoDxAnnots.parseHpoToMaxo(reader);
            Map<TermId, String> generalMaxoTermsMap = GeneralMaxoTerms.getGeneralMaxoTerms();
            Set<SimpleTermOld> generalMaxoTerms = new HashSet<>();
            generalMaxoTermsMap.entrySet().forEach(entry ->
                    generalMaxoTerms.add(new SimpleTermOld(entry.getKey(), entry.getValue())));
            for (Set<SimpleTermOld> mterms : maxoAnnotsMap.values()) {
                mterms.removeAll(generalMaxoTerms);
            }
            return maxoAnnotsMap;
        }
    }

    @Bean
    public IcMicaData icMicaData(MaxodiffDataResolver maxodiffDataResolver) throws IOException {
        Path icMicaDataPath = maxodiffDataResolver.icMicaDict().toAbsolutePath();
        if (Files.exists(icMicaDataPath)) {
            LOGGER.debug("Loading IcMicaData from {}", icMicaDataPath);
            return IcMicaDictLoader.loadIcMicaDict(maxodiffDataResolver.icMicaDict());
        } else {
            MinimalOntology hpo = minHpo(maxodiffDataResolver);
            IcMicaDictMetadata testMetadata = new IcMicaDictMetadata(hpo.version().get(), hpo.version().get(), LocalDate.now());
            Map<TermPair, Double> testIcMicaDict = new HashMap<>();
            return new IcMicaData(testIcMicaDict, testMetadata);
        }
    }

    @Bean
    public Map<TermId, Set<TermId>> hpoToMaxoIdMap(Map<SimpleTermOld, Set<SimpleTermOld>> maxoAnnotsMap) {
        Map<TermId, Set<TermId>> hpoToMaxoIdMap = new HashMap<>();
        for (Map.Entry<SimpleTermOld, Set<SimpleTermOld>> entry : maxoAnnotsMap.entrySet()) {
            TermId hpoId = entry.getKey().tid();
            Set<TermId> maxoIds = new HashSet<>();
            maxoAnnotsMap.get(entry.getKey()).forEach(t -> maxoIds.add(t.tid()));
            hpoToMaxoIdMap.put(hpoId, maxoIds);
        }
        return hpoToMaxoIdMap;
    }

    @Bean
    public BiometadataService biometadataService(
            MinimalOntology minHpo,
            HpoDiseases hpoDiseases,
            Map<SimpleTermOld, Set<SimpleTermOld>> maxoAnnotsMap) {
        return BiometadataServiceImpl.of(minHpo, hpoDiseases, maxoAnnotsMap);
    }

    @Bean
    public DiffDiagRefiner diffDiagRefiner(
            MinimalOntology minHpo,
            Ontology hpo,
            HpoDiseases hpoDiseases,
            Map<TermId, Set<TermId>> hpoToMaxoIdMap,
            Map<SimpleTermOld, Set<SimpleTermOld>> maxoAnnotsMap) {

        return new BaseDiffDiagRefiner(hpoDiseases, hpoToMaxoIdMap, maxoAnnotsMap, hpo);
    }

    @Bean
    public DifferentialDiagnosisEngineService differentialDiagnosisEngineService() {
        Map<String, DifferentialDiagnosisEngine> engines = Map.of();
        return DifferentialDiagnosisEngineServiceImpl.of(engines);
    }
}
