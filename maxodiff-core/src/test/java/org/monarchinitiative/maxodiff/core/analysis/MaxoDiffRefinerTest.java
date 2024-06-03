package org.monarchinitiative.maxodiff.core.analysis;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.TestResources;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MaxoDiffRefinerTest {

    private static Map<TermId, Set<TermId>> HPO_TO_MAXO;

    private MaxoDiffRefiner refiner;

    @BeforeAll
    public static void beforeAll() {
        HPO_TO_MAXO = new HashMap<>();

        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> e : TestResources.hpoToMaxo().entrySet()) {
            Set<TermId> values = e.getValue().stream().map(SimpleTerm::tid).collect(Collectors.toSet());
            HPO_TO_MAXO.put(e.getKey().tid(), values);
        }
    }

    @BeforeEach
    public void setUp() {
        refiner = new MaxoDiffRefiner(TestResources.hpoDiseases(), HPO_TO_MAXO, TestResources.hpo());
    }

    @Test
    public void run() {
        RefinementOptions options = RefinementOptions.of(10, .3);
        Sample sample = TestResources.getExampleSample();

        RefinementResults results = refiner.run(sample, options);

        // TODO: add assertions
        System.err.println(results);
    }

}