package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.io.MaxoDxAnnots;
import org.monarchinitiative.maxodiff.core.io.MaxodiffBuilder;
import org.monarchinitiative.maxodiff.core.io.MaxodiffDataException;
import org.monarchinitiative.maxodiff.core.io.MaxodiffDataResolver;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.monarchinitiative.maxodiff.core.io.MaxodiffBuilder.loadOntology;

// Responsibility - parse Maxo diagnostic annotations file into higher level structures.
// TODO: consider removing the class or moving to a configuration module or moving to Spring configuration/CLI superclass.
public class MaxoTermMap {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaxoTermMap.class);

    MaxodiffDataResolver dataResolver;
    Ontology hpo;
    Map<SimpleTerm, Set<SimpleTerm>> fullHpoToMaxoTermMap;

    HpoDiseases diseases;

    TermId diseaseId;

    public MaxoTermMap(Path maxoDataPath) throws MaxodiffDataException {
        this.dataResolver = new MaxodiffDataResolver(maxoDataPath);
        this.hpo = loadOntology(dataResolver.hpoJson());
        HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.defaultOptions();
        this.diseases = MaxodiffBuilder.loadHpoDiseases(dataResolver.phenotypeAnnotations(), hpo, options);
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
