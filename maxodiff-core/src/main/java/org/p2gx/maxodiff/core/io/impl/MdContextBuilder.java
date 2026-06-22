package org.p2gx.maxodiff.core.io.impl;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.cli.demo.MicaCalculator;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.MySimpleTerm;
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
        Map<MySimpleTerm, Set<MySimpleTerm>> maxoAnnotsMap = getMaxoAnnotsMap(dataDir);
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

    private static Map<MySimpleTerm, Set<MySimpleTerm>> getMaxoAnnotsMap(Path dataDir) throws IOException {
        Path maxoDxPath = Objects.requireNonNull(dataDir.resolve("maxo_diagnostic_annotations.tsv"),
                "Did not find maxo.json in data directory");
        Map<MySimpleTerm, Set<MySimpleTerm>> maxoAnnotsMap;
        try (BufferedReader reader = Files.newBufferedReader(maxoDxPath)) {
            maxoAnnotsMap = MaxoDxAnnots.parseHpoToMaxo(reader);
        }
        Map<String, String> generalMaxoTermsMap = GeneralMaxoTerms.getGeneralMaxoTerms();
        Set<MySimpleTerm> generalMaxoTerms = new HashSet<>();
        generalMaxoTermsMap.forEach((key, value) -> generalMaxoTerms.add(new MySimpleTerm(TermId.of(key), value)));
        for (Set<MySimpleTerm> mterms : maxoAnnotsMap.values()) {
            mterms.removeAll(generalMaxoTerms);
        }

        return maxoAnnotsMap;
    }

    private static BiometadataService getBiometadataService(MinimalOntology hpo,
                                                            HpoDiseases hpoDiseases,
                                                            Map<MySimpleTerm, Set<MySimpleTerm>> maxoAnnotsMap) {
        return BiometadataServiceImpl.of(hpo, hpoDiseases, maxoAnnotsMap);
    }

    private static MdResources buildMdResources(Path maxoDataPath,
                                                MinimalOntology hpo,
                                                HpoDiseases hpoDiseases,
                                                Map<MySimpleTerm, Set<MySimpleTerm>> maxoAnnotsMap,
                                                boolean buildIcData) throws IOException {

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

        IcMicaData icData = null;
        Map<TermId, Double> termToIcMap = null;
        if (buildIcData) {
            Path termPairSimFile = Objects.requireNonNull(maxoDataPath.resolve("term-pair-similarity.csv.gz"),
                    "Did not find term-pair-similarity.csv.gz in data directory");
            icData = IcMicaDictLoader.loadIcMicaDict(termPairSimFile);
            boolean assumeAnnotated = true;
            MicaCalculator micaCalculator = new MicaCalculator(hpo, assumeAnnotated);
            termToIcMap = micaCalculator.calculateMica(hpoDiseases).termToIc();
        }

        return new MdResources(hpo, hpoDiseases, maxoAnnotsMap, maxoToHpoMap, icData, termToIcMap);

    }



}
