package org.monarchinitiative.maxodiff.html.config;

import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.lirical.LiricalDifferentialDiagnosisEngineConfigurer;
import org.monarchinitiative.maxodiff.lirical.MaxodiffLiricalAnalysisRunner;
import org.monarchinitiative.maxodiff.lirical.MaxodiffLiricalAnalysisRunnerImpl;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoAssociationData;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



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
        int parallelism = 4; // TODO: extract into a property
        return MaxodiffLiricalAnalysisRunnerImpl.of(liricalPhenotypeService, parallelism);
    }

    @Bean
    public LiricalDifferentialDiagnosisEngineConfigurer liricalDifferentialDiagnosisEngineConfigurer(
            MaxodiffLiricalAnalysisRunner liricalAnalysisRunner
    ) {
        return LiricalDifferentialDiagnosisEngineConfigurer.of(liricalAnalysisRunner);
    }
}
