package org.p2gx.maxodiff.config.impl;

import org.p2gx.maxodiff.config.MaxoDiffLoader;
import org.p2gx.maxodiff.config.MaxodiffDataException;
import org.p2gx.maxodiff.config.MaxodiffDataResolver;
import org.p2gx.maxodiff.core.phenomizer.IcMicaData;
import org.p2gx.maxodiff.core.phenomizer.IcMicaDictLoader;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MaxoDiffFileLoader implements MaxoDiffLoader {
    private final static Logger LOGGER = LoggerFactory.getLogger(MaxoDiffFileLoader.class);
    private final MaxodiffDataResolver resolver;
    private MinimalOntology ontology = null;


    public MaxoDiffFileLoader(Path maxoDataPath) throws MaxodiffDataException {
        if (maxoDataPath == null) {
            throw new MaxodiffDataException("maxoDataPath cannot be null");
        }
        if (!Files.exists(maxoDataPath)) {
            throw new IllegalArgumentException("maxoDataPath does not exist: " + maxoDataPath);
        }
        this.resolver = MaxodiffDataResolver.of(maxoDataPath);
    }


    @Override
    public MinimalOntology hpo() {
        if (ontology == null) {
            Path hpoPath = resolver.hpoJson();
            ontology = MinimalOntologyLoader.loadOntology(hpoPath.toFile());
        }
        return ontology;

    }

    @Override
    public HpoDiseases hpoDiseases() throws MaxodiffDataException {
        HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(hpo(), HpoDiseaseLoaderOptions.defaultOmim());
        Path hpoaPath = resolver.phenotypeAnnotations();
        try {
            return loader.load(hpoaPath);
        } catch (IOException e) {
            LOGGER.error(e.getMessage(),e);
            throw new MaxodiffDataException(e.getMessage());
        }
    }

    @Override
    public IcMicaData icMicaData() throws MaxodiffDataException {
        try {
            return IcMicaDictLoader.loadIcMicaDict(resolver.icMicaDict());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(),e);
            throw new MaxodiffDataException(e.getMessage());
        }
    }
}
