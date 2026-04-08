package org.p2gx.maxodiff.core.io.impl;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.HTMLFrequencyMap;
import org.p2gx.maxodiff.core.analysis.MySimpleTerm;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.io.MdParams;
import org.p2gx.maxodiff.core.io.MdResources;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import org.p2gx.maxodiff.core.io.MaxoDxAnnots;
import org.p2gx.maxodiff.core.model.GeneralMaxoTerms;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.p2gx.maxodiff.core.service.BiometadataServiceImpl;
import org.p2gx.maxodiff.core.phenomizer.IcMicaData;
import org.p2gx.maxodiff.core.phenomizer.IcMicaDictLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MdContextBuilder {
    private final static Logger LOGGER = LoggerFactory.getLogger(MdContextBuilder.class);


    public static MdContext buildContext(Path maxoDataPath,
                                         int nRepetions,
                                         int nDiseases) throws Exception{
        MdParams params = new MdParams(nRepetions, nDiseases);
        Path dataDir = Objects.requireNonNull(maxoDataPath,
                "Data directory must not be null!");
        Path hpoJsonPath = Objects.requireNonNull(dataDir.resolve("hp.json"),
                "Did not find hp.json in data directory!");
        Path phenotypeHpoaPath = Objects.requireNonNull(dataDir.resolve("phenotype.hpoa"),
                "Did not find phenotype.hpoa in data directory!");
        Path maxoJsonPath = Objects.requireNonNull(dataDir.resolve("maxo.json"),
                "Did not find maxo.json in data directory");
        Path maxoDxPath = Objects.requireNonNull(dataDir.resolve("maxo_diagnostic_annotations.tsv"),
                "Did not find maxo.json in data directory");
        Path termPairSimFile = Objects.requireNonNull(dataDir.resolve("term-pair-similarity.csv.gz"),
            "Did not find term-pair-similarity.csv.gz in data directory");
        MinimalOntology hpo = MinimalOntologyLoader.loadOntology(hpoJsonPath.toFile());
        MinimalOntology maxo = MinimalOntologyLoader.loadOntology(maxoJsonPath.toFile(), "HP");
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(hpo, HpoDiseaseLoaderOptions.defaultOmim());
        HpoDiseases hpoDiseases = loader.load(phenotypeHpoaPath);
        IcMicaData icData = IcMicaDictLoader.loadIcMicaDict(termPairSimFile);
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

        BiometadataService biometadataService = BiometadataServiceImpl.of(hpo, hpoDiseases, maxoAnnotsMap);
        MdResources resources = new MdResources(hpo, hpoDiseases, maxoAnnotsMap, maxoToHpoMap, icData);

        return new MdContext(resources, params, biometadataService);
    }



}
