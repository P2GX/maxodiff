package org.monarchinitiative.maxodiff.html.config;

import org.monarchinitiative.maxodiff.config.MaxodiffDataException;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.DiffDiagRefiner;
import org.monarchinitiative.maxodiff.core.analysis.MaxoDiffRefiner;
import org.monarchinitiative.maxodiff.core.io.MaxoDxAnnots;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.core.service.BiometadataServiceImpl;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableConfigurationProperties({MaxodiffRefinerProperties.class})
public class MaxodiffRefinerConfiguration {

    private final MaxodiffRefinerProperties maxodiffRefinerProperties;

    public MaxodiffRefinerConfiguration(MaxodiffRefinerProperties maxodiffRefinerProperties) {
        this.maxodiffRefinerProperties = maxodiffRefinerProperties;
    }

    @Bean
    public Integer defaultNDiseases() {
        return maxodiffRefinerProperties.nDiseases();
    }

    @Bean
    public Double defaultWeight() {
        return maxodiffRefinerProperties.weight();
    }

    @Bean
    public Integer defaultNMaxoResults() {
        return maxodiffRefinerProperties.nMaxoResults();
    }
}
