package org.monarchinitiative.maxodiff.core;

import org.monarchinitiative.maxodiff.core.io.MaxoDxAnnots;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Utility class with lazily-loaded resources for testing
 */
public class TestResources {

    public static final Path TEST_BASE = Path.of("src/test/resources");
    private static final Path HPO_PATH = TestResources.TEST_BASE.resolve("hp.v2024-04-26.json.gz");
    private static final Path ANNOTATION_PATH = TestResources.TEST_BASE.resolve("small.hpoa");
    private static final Path MAXO_DIAGNOSTIC_ANNOTATIONS_PATH = TestResources.TEST_BASE.resolve("maxo_diagnostic_annotations.v2023-06-11.tsv.gz");
    // The HPO is in the default  curie map and only contains known relationships / HP terms
    private static volatile Ontology ONTOLOGY;
    private static volatile HpoDiseases HPO_DISEASES;
    private static volatile Map<SimpleTerm, Set<SimpleTerm>> HPO_2_MAXO;

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

    public static Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxo() {
        if (HPO_2_MAXO == null) {
            synchronized (TestResources.class) {
                if (HPO_2_MAXO == null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(MAXO_DIAGNOSTIC_ANNOTATIONS_PATH))))) {
                        HPO_2_MAXO = MaxoDxAnnots.parseHpoToMaxo(reader);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return HPO_2_MAXO;
    }

    private TestResources() {
    }
}

