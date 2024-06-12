package org.monarchinitiative.maxodiff.html.config;

import org.monarchinitiative.maxodiff.core.io.MaxoDxAnnots;
import org.monarchinitiative.maxodiff.config.MaxodiffDataException;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.DiffDiagRefiner;
import org.monarchinitiative.maxodiff.core.analysis.MaxoDiffRefiner;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.core.service.BiometadataServiceImpl;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(MaxodiffProperties.class)
public class MaxodiffConfiguration {

    private final MaxodiffProperties maxodiffProperties;

    public MaxodiffConfiguration(MaxodiffProperties maxodiffProperties) {
        this.maxodiffProperties = maxodiffProperties;
    }

    @Bean
    public MaxodiffDataResolver maxodiffDataResolver() throws MaxodiffDataException {
        return MaxodiffDataResolver.of(maxodiffProperties.maxodiffDataDir());
    }

    @Bean
    public MinimalOntology hpo(MaxodiffDataResolver maxodiffDataResolver) {
        return MinimalOntologyLoader.loadOntology(maxodiffDataResolver.hpoJson().toFile());
    }

    @Bean
    public HpoDiseases hpoDiseases(MinimalOntology hpo, MaxodiffDataResolver maxodiffDataResolver) throws IOException {
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(hpo, HpoDiseaseLoaderOptions.defaultOptions());
        return loader.load(maxodiffDataResolver.phenotypeAnnotations());
    }

    @Bean
    public Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap(MaxodiffDataResolver maxodiffDataResolver) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(maxodiffDataResolver.maxoDxAnnots())) {
            return MaxoDxAnnots.parseHpoToMaxo(reader);
        }
    }

    @Bean
    public BiometadataService biometadataService(
            MinimalOntology hpo,
            HpoDiseases hpoDiseases,
            Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap) {

        Map<TermId, String> hpoToLabel = hpo.getTerms().stream().collect(Collectors.toMap(Term::id, Term::getName));
        Map<TermId, String> diseaseToLabel = hpoDiseases.hpoDiseases().collect(Collectors.toMap(HpoDisease::id, HpoDisease::diseaseName));
        // Note, we assume that there are no MAxO terms with identical ids but different labels.
        Map<String, String> maxoTermsMap = maxoAnnotsMap.values().stream()
                .flatMap(Collection::stream).distinct()
                .collect(Collectors.toMap(t -> t.tid().getValue(), SimpleTerm::label));
        return new BiometadataServiceImpl(maxoTermsMap, hpoToLabel, diseaseToLabel);
    }

    @Bean
    public DiffDiagRefiner diffDiagRefiner(
            MinimalOntology hpo,
            HpoDiseases hpoDiseases,
            Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap) {

        Map<TermId, Set<TermId>> hpoToMaxoIdMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : maxoAnnotsMap.entrySet()) {
            TermId hpoId = entry.getKey().tid();
            Set<TermId> maxoIds = new HashSet<>();
            maxoAnnotsMap.get(entry.getKey()).forEach(t -> maxoIds.add(t.tid()));
            hpoToMaxoIdMap.put(hpoId, maxoIds);
        }
        return new MaxoDiffRefiner(hpoDiseases, hpoToMaxoIdMap, hpo);
    }
}
