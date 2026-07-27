package org.p2gx.maxodiff.core.io.impl;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.cli.demo.MicaCalculator;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.io.MdParams;
import org.p2gx.maxodiff.core.io.MdResources;
import org.p2gx.maxodiff.core.io.MaxoDxAnnots;
import org.p2gx.maxodiff.core.model.GeneralMaxoTerms;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.p2gx.maxodiff.core.service.BiometadataServiceImpl;
import org.p2gx.maxodiff.core.phenomizer.IcMicaData;
import org.p2gx.maxodiff.core.phenomizer.IcMicaDictLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MdContextBuilder {
    private final static Logger LOGGER = LoggerFactory.getLogger(MdContextBuilder.class);


    public static MdContext buildContext(Path maxoDataPath,
                                         int nRepetitions,
                                         int nDiseases,
                                         boolean buildICData) throws Exception {

        MdParams params = new MdParams(nRepetitions, nDiseases);
        Path dataDir = Objects.requireNonNull(maxoDataPath,
                "Data directory must not be null!");
        MinimalOntology hpo = getHpo(dataDir);
        HpoDiseases hpoDiseases = getHpoDiseases(dataDir, hpo);
        Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap = getMaxoAnnotsMap(dataDir);
        BiometadataService biometadataService = getBiometadataService(hpo, hpoDiseases, maxoAnnotsMap);
        MdResources resources = buildMdResources(maxoDataPath, hpo, hpoDiseases, maxoAnnotsMap, buildICData);
        return new MdContext(resources, params, biometadataService);
    }

    private static MinimalOntology getHpo(Path dataDir) {
        Path hpoJsonPath = Objects.requireNonNull(dataDir.resolve("hp.json"),
                "Did not find hp.json in data directory!");
        return MinimalOntologyLoader.loadOntology(hpoJsonPath.toFile());
    }

    private static HpoDiseases getHpoDiseases(Path dataDir,
                                              MinimalOntology hpo) throws IOException {
        Path phenotypeHpoaPath = Objects.requireNonNull(dataDir.resolve("phenotype.hpoa"),
                "Did not find phenotype.hpoa in data directory!");
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(hpo, HpoDiseaseLoaderOptions.defaultOmim());
        return loader.load(phenotypeHpoaPath);
    }

    private static Map<SimpleTerm, Set<SimpleTerm>> getMaxoAnnotsMap(Path dataDir) throws IOException {
        Path maxoDxPath = Objects.requireNonNull(dataDir.resolve("maxo_diagnostic_annotations.tsv"),
                "Did not find maxo.json in data directory");
        Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap;
        try (BufferedReader reader = Files.newBufferedReader(maxoDxPath)) {
            maxoAnnotsMap = MaxoDxAnnots.parseHpoToMaxo(reader);
        }
        Map<String, String> generalMaxoTermsMap = GeneralMaxoTerms.getGeneralMaxoTerms();
        Set<SimpleTerm> generalMaxoTerms = new HashSet<>();
        generalMaxoTermsMap.forEach((key, value) -> generalMaxoTerms.add(new SimpleTerm(TermId.of(key), value)));
        for (Set<SimpleTerm> mterms : maxoAnnotsMap.values()) {
            mterms.removeAll(generalMaxoTerms);
        }

        return maxoAnnotsMap;
    }

    private static BiometadataService getBiometadataService(MinimalOntology hpo,
                                                            HpoDiseases hpoDiseases,
                                                            Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap) {
        return BiometadataServiceImpl.of(hpo, hpoDiseases, maxoAnnotsMap);
    }

    private static MdResources buildMdResources(Path maxoDataPath,
                                                MinimalOntology hpo,
                                                HpoDiseases hpoDiseases,
                                                Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap,
                                                boolean buildIcData) throws IOException, ClassNotFoundException {

        Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> e : maxoAnnotsMap.entrySet()) {
            SimpleTerm hpoTerm = e.getKey();
            SimpleTerm hpoIdTerm = new SimpleTerm(hpoTerm.tid(), hpoTerm.label());
            Set<SimpleTerm> maxoTerms = e.getValue();
            for (SimpleTerm maxoTerm : maxoTerms) {
                SimpleTerm maxoIdTerm = new SimpleTerm(maxoTerm.tid(), maxoTerm.label());
                if (!maxoToHpoMap.containsKey(maxoIdTerm)) {
                    maxoToHpoMap.put(maxoIdTerm, new HashSet<>(Collections.singleton(hpoIdTerm)));
                } else {
                    Set<SimpleTerm> hpoIdTerms = maxoToHpoMap.get(maxoIdTerm);
                    hpoIdTerms.add(hpoIdTerm);
                    maxoToHpoMap.replace(maxoIdTerm, hpoIdTerms);
                }
            }
        }

        IcMicaData icData = null;
        Map<TermId, Double> termToIcMap = null;
        if (buildIcData) {
            Path termPairSimFile = Objects.requireNonNull(maxoDataPath.resolve("term-pair-similarity.ser"),
                    "Did not find term-pair-similarity.ser in data directory");
            icData = IcMicaDictLoader.loadIcMicaDictSer(termPairSimFile);
            boolean assumeAnnotated = true;
            MicaCalculator micaCalculator = new MicaCalculator(hpo, assumeAnnotated);
            termToIcMap = micaCalculator.calculateMica(hpoDiseases).termToIc();
        }

        return new MdResources(hpo, hpoDiseases, maxoAnnotsMap, maxoToHpoMap, icData, termToIcMap);

    }



}
