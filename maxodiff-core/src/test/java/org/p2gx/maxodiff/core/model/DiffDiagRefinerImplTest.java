package org.p2gx.maxodiff.core.analysis.refinement;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.MaxodiffRuntimeException;
import org.p2gx.maxodiff.core.TestResources;
import org.p2gx.maxodiff.core.analysis.HpoFrequency;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.io.MdParams;
import org.p2gx.maxodiff.core.io.MdResources;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DiffDiagRefinerImplTest {

    private static final Map<SimpleTerm, Set<SimpleTerm>> SOME_ANNOTATIONS = Map.of(
            new SimpleTerm(TermId.of("HP:0000001"), "an HPO term"),
            Set.of(new SimpleTerm(TermId.of("MAXO:0000001"), "a MAxO term")));

    /** Only the two maps matter to the constructor, so the other resources can be null. */
    private static MdContext contextWith(Map<SimpleTerm, Set<SimpleTerm>> maxoAnnots,
                                         Map<SimpleTerm, Set<SimpleTerm>> maxoToHpo) {
        MdResources resources = new MdResources(null, null, maxoAnnots, maxoToHpo, null, Map.of());
        return new MdContext(resources, MdParams.defaultParams(), null);
    }

    @Test
    void anEmptyMaxoToHpoMapIsReportedWithAnException() {
        MdContext context = contextWith(SOME_ANNOTATIONS, Map.of());
        assertThrows(MaxodiffRuntimeException.class, () -> new DiffDiagRefinerImpl(context));
    }

    @Test
    void anEmptyHpoToMaxoMapIsReportedWithAnException() {
        MdContext context = contextWith(Map.of(), SOME_ANNOTATIONS);
        assertThrows(MaxodiffRuntimeException.class, () -> new DiffDiagRefinerImpl(context));
    }

    @Test
    void bothMapsPresentIsFine() {
        MdContext context = contextWith(SOME_ANNOTATIONS, SOME_ANNOTATIONS);
        assertDoesNotThrow(() -> new DiffDiagRefinerImpl(context));
    }

    private static List<HpoDisease> firstDiseases(int n) {
        return TestResources.hpoDiseases().stream().limit(n).toList();
    }

    private static HpoFrequency freq(HpoDisease disease, int hpoNumber) {
        return new HpoFrequency(disease.id(), TermId.of(String.format("HP:%07d", hpoNumber)), 0.5f, 1f);
    }

    private static DiffDiagRefinerImpl refiner() {
        return new DiffDiagRefinerImpl(contextWith(SOME_ANNOTATIONS, SOME_ANNOTATIONS));
    }

    @Test
    void onlyRecordsOfTheGivenDiseasesAreKeptInTheirOriginalOrder() {
        List<HpoDisease> three = firstDiseases(3);
        HpoDisease a = three.get(0), b = three.get(1), c = three.get(2);
        List<HpoFrequency> all = List.of(freq(a, 1), freq(b, 2), freq(c, 3), freq(a, 4), freq(c, 5));

        List<HpoFrequency> kept = refiner().getHpoFrequenciesNDiseases(List.of(a, c), all);

        assertEquals(List.of(all.get(0), all.get(2), all.get(3), all.get(4)), kept);
    }

    @Test
    void noDiseasesGivesNoRecords() {
        HpoDisease a = firstDiseases(1).get(0);
        assertEquals(List.of(), refiner().getHpoFrequenciesNDiseases(List.of(), List.of(freq(a, 1))));
    }

    @Test
    void noRecordsGivesNoRecords() {
        HpoDisease a = firstDiseases(1).get(0);
        assertEquals(List.of(), refiner().getHpoFrequenciesNDiseases(List.of(a), List.of()));
    }

    @Test
    void aDiseaseWithoutRecordsContributesNothing() {
        List<HpoDisease> two = firstDiseases(2);
        HpoFrequency onlyForFirst = freq(two.get(0), 1);

        assertEquals(List.of(onlyForFirst),
                refiner().getHpoFrequenciesNDiseases(two, List.of(onlyForFirst)));
    }
}