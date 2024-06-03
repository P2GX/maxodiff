package org.monarchinitiative.maxodiff.core;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Utility class with lazily-loaded resources for testing
 */
public class TestResources {

    public static final Path TEST_BASE = Path.of("src/test/resources");
    private static final Path HPO_PATH = TestResources.TEST_BASE.resolve("hp.v2024-04-26.json.gz");
    private static final Path ANNOTATION_PATH = TestResources.TEST_BASE.resolve("small.hpoa");
    // The HPO is in the default  curie map and only contains known relationships / HP terms
    private static volatile Ontology ONTOLOGY;
    private static volatile HpoDiseases HPO_DISEASES;

    public static HpoDiseases hpoDiseases() {
        if (HPO_DISEASES == null) {
            synchronized (TestResources.class) {
                if (HPO_DISEASES == null)
                    HPO_DISEASES = loadHpoDiseases();
            }
        }
        return HPO_DISEASES;
    }

    private static HpoDiseases loadHpoDiseases() {
        try {
            HpoDiseaseLoaderOptions options = HpoDiseaseLoaderOptions.of(Set.of(DiseaseDatabase.OMIM), true, HpoDiseaseLoaderOptions.DEFAULT_COHORT_SIZE);
            HpoDiseaseLoader loader = HpoDiseaseLoaders.defaultLoader(hpo(), options);
            return loader.load(ANNOTATION_PATH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Ontology hpo() {
        if (ONTOLOGY == null) {
            synchronized (TestResources.class) {
                if (ONTOLOGY == null) {
                    try (InputStream reader = new GZIPInputStream(Files.newInputStream(HPO_PATH))) {
                        ONTOLOGY = OntologyLoader.loadOntology(reader);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return ONTOLOGY;
    }

    private TestResources() {
    }
}

