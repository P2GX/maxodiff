package org.monarchinitiative.maxodiff.config;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.io.MaxoDxAnnots;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

// Responsibility - parse Maxo diagnostic annotations file into higher level structures.
// TODO: consider removing the class or moving to a configuration module or moving to Spring configuration/CLI superclass.
public class MaxoTermMap {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaxoTermMap.class);

    MaxodiffDataResolver dataResolver;
    MinimalOntology hpo;
    Map<SimpleTerm, Set<SimpleTerm>> fullHpoToMaxoTermMap;

    HpoDiseases diseases;

    TermId diseaseId;

    public MaxoTermMap(Path maxoDataPath) throws MaxodiffDataException {
        this.dataResolver = MaxodiffDataResolver.of(maxoDataPath);
        this.hpo = MinimalOntologyLoader.loadOntology(dataResolver.hpoJson().toFile());
        HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.defaultOptions();
        try {
            HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(hpo, options);
            this.diseases = loader.load(dataResolver.phenotypeAnnotations());
        } catch (IOException ex) {
            throw new MaxodiffDataException(ex);
        }

        try (BufferedReader reader = Files.newBufferedReader(dataResolver.maxoDxAnnots())) {
            this.fullHpoToMaxoTermMap = MaxoDxAnnots.parseHpoToMaxo(reader);
        } catch (IOException e) {
            throw new MaxodiffDataException(e);
        }
    }

    public TermId getDiseaseId() {
        return diseaseId;
    }

    public HpoDiseases getDiseases() {
        return diseases;
    }

    public Map<SimpleTerm, Set<SimpleTerm>> getFullHpoToMaxoTermMap() {
        return fullHpoToMaxoTermMap;
    }

    public Map<TermId, Set<TermId>> getFullHpoToMaxoTermIdMap(Map<SimpleTerm, Set<SimpleTerm>> fullHpoToMaxoTermMap) {
        Map<TermId, Set<TermId>> fullHpoToMaxoTermIdMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : fullHpoToMaxoTermMap.entrySet()) {
            SimpleTerm hpoTerm = entry.getKey();
            Set<SimpleTerm> maxoTerms = entry.getValue();
            Set<TermId> maxoTermIds = new HashSet<>();
            maxoTerms.forEach(t -> maxoTermIds.add(t.tid()));
            fullHpoToMaxoTermIdMap.put(hpoTerm.tid(), maxoTermIds);
        }
        return fullHpoToMaxoTermIdMap;
    }

    public MinimalOntology getOntology() {
        return hpo;
    }

}
