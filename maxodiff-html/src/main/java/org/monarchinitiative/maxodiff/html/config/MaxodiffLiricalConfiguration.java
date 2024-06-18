package org.monarchinitiative.maxodiff.html.config;

import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.maxodiff.config.MaxodiffDataException;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.DiffDiagRefiner;
import org.monarchinitiative.maxodiff.core.analysis.MaxoDiffRefiner;
import org.monarchinitiative.maxodiff.core.io.MaxoDxAnnots;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.core.service.BiometadataServiceImpl;
import org.monarchinitiative.maxodiff.lirical.LiricalRecord;
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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@EnableConfigurationProperties({MaxodiffProperties.class, MaxodiffLiricalProperties.class})
public class MaxodiffLiricalConfiguration {

    private final MaxodiffProperties maxodiffProperties;
    private final MaxodiffLiricalProperties maxodiffLiricalProperties;

    public MaxodiffLiricalConfiguration(MaxodiffProperties maxodiffProperties,
                                        MaxodiffLiricalProperties maxodiffLiricalProperties) {
        this.maxodiffProperties = maxodiffProperties;
        this.maxodiffLiricalProperties = maxodiffLiricalProperties;
    }

    @Bean
    public Path liricalDataDir() {
        return maxodiffProperties.liricalDataDir();
    }

    @Bean
    public Path maxodiffDataDir() {
        return maxodiffProperties.maxodiffDataDir();
    }

    @Bean
    public LiricalRecord defaultLiricalRecord() {
        Path liricalDataDir = liricalDataDir();

        String genomeBuild = maxodiffLiricalProperties.genomeBuild();
        TranscriptDatabase transcriptDatabase = maxodiffLiricalProperties.transcriptDatabase();
        Float pathogenicityThreshold = maxodiffLiricalProperties.pathogenicityThreshold();
        Double defaultVariantBackgroundFrequency = maxodiffLiricalProperties.defaultVariantBackgroundFrequency();
        boolean strict = maxodiffLiricalProperties.strict();
        boolean globalAnalysisMode = maxodiffLiricalProperties.globalMode();

        return new LiricalRecord(genomeBuild, transcriptDatabase, pathogenicityThreshold,
                defaultVariantBackgroundFrequency, strict, globalAnalysisMode, liricalDataDir, null, null);
    }
}
