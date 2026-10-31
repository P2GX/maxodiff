package org.p2gx.maxodiff.core.analysis.refinement;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.MaxodiffRuntimeException;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.io.MdParams;
import org.p2gx.maxodiff.core.io.MdResources;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}