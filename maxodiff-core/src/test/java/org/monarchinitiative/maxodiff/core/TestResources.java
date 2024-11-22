package org.monarchinitiative.maxodiff.core;

import org.monarchinitiative.maxodiff.core.io.MaxoDxAnnots;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoader;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaderOptions;
import org.monarchinitiative.phenol.annotations.io.hpo.HpoDiseaseLoaders;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Utility class with lazily-loaded resources for testing
 */
public class TestResources {

    public static final Path TEST_BASE = Path.of("src/test/resources");
    private static final Path HPO_PATH = TestResources.TEST_BASE.resolve("hp.v2024-04-26.json.gz");
    private static final Path ANNOTATION_PATH = TestResources.TEST_BASE.resolve("phenotype.v2024-01-16.hpoa.gz");
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

    public static Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoToy() {
        Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoToy = new HashMap<>();
        List<TermId> hpoIdSubset = List.of(
            TermId.of("HP:0006739"),
            TermId.of("HP:0002863"),
            TermId.of("HP:0100651"),
            TermId.of("HP:0031548"),
            TermId.of("HP:0031549"),
            TermId.of("HP:0002860"),
                TermId.of("HP:0001888"),
                TermId.of("HP:0001903"),
                TermId.of("HP:0001873")
        );

        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : hpoToMaxo().entrySet()) {
            SimpleTerm hpoTerm = entry.getKey();
            Set<SimpleTerm> maxoTerms = entry.getValue();
            for (TermId hpoId : hpoIdSubset) {
                if (hpoTerm.tid().equals(hpoId)) {
                    hpoToMaxoToy.put(hpoTerm, maxoTerms);
                }
            }
        }

        return hpoToMaxoToy;
    }

    public static Sample getExampleSample() {
        //Example terms from phenopacket v2 PMID_11175294-Tiecke-2001-FBN1-B15.json
        Collection<TermId> presentTerms = List.of(
                TermId.of("HP:0000963"),
                TermId.of("HP:0001653"),
                TermId.of("HP:0000545"),
                TermId.of("HP:0000098"),
                TermId.of("HP:0004325"),
                TermId.of("HP:0002751"),
                TermId.of("HP:0002650"),
                TermId.of("HP:0002616"),
                TermId.of("HP:0000767"),
                TermId.of("HP:0012019"),
                TermId.of("HP:0001166")
        );
        Collection<TermId> excludedTerms = List.of();

        return Sample.of("B15", presentTerms, excludedTerms);
    }

    public static Collection<DifferentialDiagnosis> getExampleDiagnoses() {
        //Example Top 20 Diagnoses from LIRICAL analysis of phenopacket v2 PMID_11175294-Tiecke-2001-FBN1-B15.json
        //score = posttest probability
        return List.of(
                DifferentialDiagnosis.of(TermId.of("OMIM:154700"), 1.0, 12.966),
                DifferentialDiagnosis.of(TermId.of("OMIM:616914"), 1.000, 10.165),
                DifferentialDiagnosis.of(TermId.of("OMIM:236200"), 1.000, 9.804),
                DifferentialDiagnosis.of(TermId.of("OMIM:609008"), 1.000, 8.548),
                DifferentialDiagnosis.of(TermId.of("OMIM:615582"), 1.000, 8.452),
                DifferentialDiagnosis.of(TermId.of("OMIM:121050"), 0.987, 5.977),
                DifferentialDiagnosis.of(TermId.of("OMIM:614816"), 0.868, 4.914),
                DifferentialDiagnosis.of(TermId.of("OMIM:617506"), 0.796, 4.687),
                DifferentialDiagnosis.of(TermId.of("OMIM:219150"), 0.767, 4.614),
                DifferentialDiagnosis.of(TermId.of("OMIM:608328"), 0.756, 4.587),
                DifferentialDiagnosis.of(TermId.of("OMIM:610443"), 0.580, 4.235),
                DifferentialDiagnosis.of(TermId.of("OMIM:277600"), 0.352, 3.831),
                DifferentialDiagnosis.of(TermId.of("OMIM:271640"), 0.282, 3.689),
                DifferentialDiagnosis.of(TermId.of("OMIM:602535"), 0.216, 3.537),
                DifferentialDiagnosis.of(TermId.of("OMIM:619472"), 0.199, 3.492),
                DifferentialDiagnosis.of(TermId.of("OMIM:225400"), 0.125, 3.250),
                DifferentialDiagnosis.of(TermId.of("OMIM:601776"), 0.070, 2.970),
                DifferentialDiagnosis.of(TermId.of("OMIM:617602"), 0.061, 2.912),
                DifferentialDiagnosis.of(TermId.of("OMIM:163950"), 0.025, 2.505),
                DifferentialDiagnosis.of(TermId.of("OMIM:208050"), 0.009, 2.064)
        );
    }

    private TestResources() {
    }
}

