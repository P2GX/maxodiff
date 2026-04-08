package org.p2gx.maxodiff.html.config;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.MySimpleTerm;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefinerImpl;
import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.p2gx.maxodiff.core.io.MaxoDxAnnots;
import org.p2gx.maxodiff.config.MaxodiffDataException;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.p2gx.maxodiff.config.MaxodiffDataResolver;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.io.MdParams;
import org.p2gx.maxodiff.core.io.MdResources;
import org.p2gx.maxodiff.core.model.GeneralMaxoTerms;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.p2gx.maxodiff.core.service.BiometadataServiceImpl;
import org.p2gx.maxodiff.html.service.DifferentialDiagnosisEngineService;
import org.p2gx.maxodiff.html.service.DifferentialDiagnosisEngineServiceImpl;
import org.p2gx.maxodiff.core.phenomizer.IcMicaData;
import org.p2gx.maxodiff.core.phenomizer.IcMicaDictLoader;
import org.p2gx.maxodiff.core.phenomizer.IcMicaDictMetadata;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;
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
    public Map<MySimpleTerm, Set<MySimpleTerm>> maxoAnnotsMap(MaxodiffDataResolver maxodiffDataResolver) throws IOException {
        LOGGER.debug("Loading MAxO annotations from {}", maxodiffDataResolver.maxoDxAnnots().toAbsolutePath());
        try (BufferedReader reader = Files.newBufferedReader(maxodiffDataResolver.maxoDxAnnots())) {
            Map<MySimpleTerm, Set<MySimpleTerm>> maxoAnnotsMap = MaxoDxAnnots.parseHpoToMaxo(reader);
            Map<String, String> generalMaxoTermsMap = GeneralMaxoTerms.getGeneralMaxoTerms();
            Set<MySimpleTerm> generalMaxoTerms = new HashSet<>();
            generalMaxoTermsMap.forEach((key, value) -> generalMaxoTerms.add(new MySimpleTerm(TermId.of(key), value)));
            for (Set<MySimpleTerm> mterms : maxoAnnotsMap.values()) {
                mterms.removeAll(generalMaxoTerms);
            }
            return maxoAnnotsMap;
        }
    }

    @Bean
    public Map<MySimpleTerm, Set<MySimpleTerm>> maxoToHpoMap(Map<MySimpleTerm, Set<MySimpleTerm>> maxoAnnotsMap) throws IOException {
        Map<MySimpleTerm, Set<MySimpleTerm>> maxoToHpoMap = new HashMap<>();
        for (Map.Entry<MySimpleTerm, Set<MySimpleTerm>> e : maxoAnnotsMap.entrySet()) {
            MySimpleTerm hpoTerm = e.getKey();
            MySimpleTerm hpoIdTerm = new MySimpleTerm(hpoTerm.tid(), hpoTerm.label());
            Set<MySimpleTerm> maxoTerms = e.getValue();
            for (MySimpleTerm maxoTerm : maxoTerms) {
                MySimpleTerm maxoIdTerm = new MySimpleTerm(maxoTerm.tid(), maxoTerm.label());
                if (!maxoToHpoMap.containsKey(maxoIdTerm)) {
                    maxoToHpoMap.put(maxoIdTerm, new HashSet<>(Collections.singleton(hpoIdTerm)));
                } else {
                    Set<MySimpleTerm> hpoIdTerms = maxoToHpoMap.get(maxoIdTerm);
                    hpoIdTerms.add(hpoIdTerm);
                    maxoToHpoMap.replace(maxoIdTerm, hpoIdTerms);
                }
            }
        }
        return maxoToHpoMap;
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
    public Map<String, Set<String>> hpoToMaxoIdMap(Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap) {
        Map<String, Set<String>> hpoToMaxoIdMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : maxoAnnotsMap.entrySet()) {
            String hpoId = entry.getKey().termId();
            Set<String> maxoIds = new HashSet<>();
            maxoAnnotsMap.get(entry.getKey()).forEach(t -> maxoIds.add(t.termId()));
            hpoToMaxoIdMap.put(hpoId, maxoIds);
        }
        return hpoToMaxoIdMap;
    }

    @Bean
    public BiometadataService biometadataService(
            MinimalOntology minHpo,
            HpoDiseases hpoDiseases,
            Map<MySimpleTerm, Set<MySimpleTerm>> maxoAnnotsMap) {
        return BiometadataServiceImpl.of(minHpo, hpoDiseases, maxoAnnotsMap);
    }

    @Bean
    public MdContext mdContext(MinimalOntology hpo,
                               HpoDiseases hpoDiseases,
                               Map<MySimpleTerm, Set<MySimpleTerm>> maxoAnnotsMap,
                               Map<MySimpleTerm, Set<MySimpleTerm>> maxoToHpoMap,
                               IcMicaData icMicaData) {
        MdParams params = MdParams.defaultParams();
        MdResources resources = new MdResources(hpo, hpoDiseases, maxoAnnotsMap, maxoToHpoMap, icMicaData);
        BiometadataService biometadataService = BiometadataServiceImpl.of(hpo, hpoDiseases, maxoAnnotsMap);
        return new MdContext(resources, params, biometadataService);
    }


    @Bean
    public DiffDiagRefiner diffDiagRefiner(
            MdContext mdContext,
            Map<MySimpleTerm, Set<MySimpleTerm>> maxoToHpoMap,
            Map<MySimpleTerm, Set<MySimpleTerm>> maxoAnnotsMap) {

        return new DiffDiagRefinerImpl(mdContext, maxoToHpoMap, maxoAnnotsMap);
    }

    @Bean
    public DifferentialDiagnosisEngineService differentialDiagnosisEngineService() {
        Map<String, DDxEngine> engines = Map.of();
        return DifferentialDiagnosisEngineServiceImpl.of(engines);
    }
}
