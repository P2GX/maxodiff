package org.p2gx.maxodiff.html.config;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.config.MaxodiffDataException;
import org.p2gx.maxodiff.core.analysis.MySimpleTerm;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.io.impl.MdContextBuilder;
import org.p2gx.maxodiff.core.phenomizer.IcMicaData;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.p2gx.maxodiff.html.service.DifferentialDiagnosisEngineService;
import org.p2gx.maxodiff.html.service.DifferentialDiagnosisEngineServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    public MdContext mdContext(Path maxodiffDataDirectory) throws Exception {
        return MdContextBuilder.buildContext(maxodiffDataDirectory, 100, 20, true);
    }

    @Bean
    public MinimalOntology minHpo(MdContext mdContext) {
        return mdContext.resources().hpo();
    }

    @Bean
    public HpoDiseases hpoDiseases(MdContext mdContext){
        return mdContext.resources().hpoDiseases();
    }

    @Bean
    public Map<MySimpleTerm, Set<MySimpleTerm>> maxoAnnotsMap(MdContext mdContext) {
        return mdContext.resources().maxoAnnotsMap();
    }

    @Bean
    public Map<MySimpleTerm, Set<MySimpleTerm>> maxoToHpoMap(MdContext mdContext) {
        return mdContext.resources().maxoToHpoMap();
    }

    @Bean
    public IcMicaData icMicaData(MdContext mdContext) {
        return mdContext.resources().icMicaData();
    }

    @Bean
    public Map<TermId, Set<TermId>> hpoToMaxoIdMap(Map<MySimpleTerm, Set<MySimpleTerm>> maxoAnnotsMap) {
        Map<TermId, Set<TermId>> hpoToMaxoIdMap = new HashMap<>();
        for (Map.Entry<MySimpleTerm, Set<MySimpleTerm>> entry : maxoAnnotsMap.entrySet()) {
            TermId hpoId = entry.getKey().tid();
            Set<TermId> maxoIds = new HashSet<>();
            maxoAnnotsMap.get(entry.getKey()).forEach(t -> maxoIds.add(t.tid()));
            hpoToMaxoIdMap.put(hpoId, maxoIds);
        }
        return hpoToMaxoIdMap;
    }

    @Bean
    public BiometadataService biometadataService(MdContext mdContext) {
        return mdContext.biometadataService();
    }


    @Bean
    public DiffDiagRefiner diffDiagRefiner(MdContext mdContext) {
        return mdContext.createRefiner();
    }

    @Bean
    public DifferentialDiagnosisEngineService differentialDiagnosisEngineService() {
        Map<String, DDxEngine> engines = Map.of();
        return DifferentialDiagnosisEngineServiceImpl.of(engines);
    }
}
