package org.monarchinitiative.maxodiff.html.config;

import org.monarchinitiative.lirical.configuration.impl.BundledBackgroundVariantFrequencyServiceFactory;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.lirical.core.service.VariantMetadataServiceFactory;
import org.monarchinitiative.lirical.exomiser_db_adapter.ExomiserMvStoreMetadataServiceFactory;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.core.lirical.LiricalDifferentialDiagnosisEngineConfigurer;
import org.monarchinitiative.maxodiff.core.lirical.MaxodiffLiricalAnalysisRunner;
import org.monarchinitiative.maxodiff.core.lirical.MaxodiffLiricalAnalysisRunnerImpl;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableConfigurationProperties({LiricalProperties.class})
public class LiricalAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalAutoConfiguration.class);

    @Bean
    public HpoAssociationData hpoAssociationData(
            MinimalOntology hpo,
            HpoDiseases hpoDiseases,
            MaxodiffDataResolver maxodiffDataResolver
    ) {
        LOGGER.debug("Loading HGNC complete set file at {}", maxodiffDataResolver.hgncCompleteSet().toAbsolutePath());
        LOGGER.debug("Loading mim2gene medgen file at {}", maxodiffDataResolver.mim2geneMedgen().toAbsolutePath());
        return HpoAssociationData.builder(hpo)
                .hgncCompleteSetArchive(maxodiffDataResolver.hgncCompleteSet())
                .mim2GeneMedgen(maxodiffDataResolver.mim2geneMedgen())
                .hpoDiseases(hpoDiseases)
                .build();
    }

    @Bean
    public PhenotypeService liricalPhenotypeService(
            MinimalOntology hpo,
            HpoDiseases hpoDiseases,
            HpoAssociationData hpoAssociationData
    ) {
        return PhenotypeService.of(hpo, hpoDiseases, hpoAssociationData);
    }

    @Bean
    public MaxodiffLiricalAnalysisRunner liricalAnalysisRunner(PhenotypeService liricalPhenotypeService) {
        BundledBackgroundVariantFrequencyServiceFactory factory = BundledBackgroundVariantFrequencyServiceFactory.getInstance();
        int parallelism = 4; // TODO: extract into a property
        return MaxodiffLiricalAnalysisRunnerImpl.of(liricalPhenotypeService, factory, parallelism);
    }

    @Bean
    public VariantMetadataServiceFactory variantMetadataServiceFactory(LiricalProperties liricalProperties) {
        Map<GenomeBuild, Path> exomiserDbPaths = new HashMap<>();

        // Check HG19
        checkExomiserDbPath(
                GenomeBuild.HG19,
                liricalProperties.getExomiserHg19Path(),
                exomiserDbPaths
        );

        // Check HG38
        checkExomiserDbPath(
                GenomeBuild.HG38,
                liricalProperties.getExomiserHg38Path(),
                exomiserDbPaths
        );

        return ExomiserMvStoreMetadataServiceFactory.of(exomiserDbPaths);
    }

    private static void checkExomiserDbPath(
            GenomeBuild genomeBuild,
            String pathString,
            Map<GenomeBuild, Path> exomiserDbPaths
    ) {
        if (pathString != null) {
            Path path = Path.of(pathString);
            if (Files.isRegularFile(path)) {
                LOGGER.debug("Using Exomiser database at {} for {}", path.toAbsolutePath(), genomeBuild);
                exomiserDbPaths.put(genomeBuild, path);
            } else {
                throw new RuntimeException("Not a file: %s".formatted(path.toAbsolutePath()));
            }
        }
    }

    @Bean
    public LiricalDifferentialDiagnosisEngineConfigurer liricalDifferentialDiagnosisEngineConfigurer(
            MaxodiffLiricalAnalysisRunner liricalAnalysisRunner
    ) {
        return LiricalDifferentialDiagnosisEngineConfigurer.of(liricalAnalysisRunner);
    }
}
