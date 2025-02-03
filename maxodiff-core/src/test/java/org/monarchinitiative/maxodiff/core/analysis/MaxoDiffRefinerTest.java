package org.monarchinitiative.maxodiff.core.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.TestResources;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxoDiffRefiner;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementOptions;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementResults;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        refiner = new MaxoDiffRefiner(TestResources.hpoDiseases(), HPO_TO_MAXO, TestResources.hpoToMaxo(), TestResources.hpo());
    }

    @Test
    public void run() {
        RefinementOptions options = RefinementOptions.of(12, 0.5);
        Sample sample = TestResources.getExampleSample();
        Collection<DifferentialDiagnosis> originalDiagnoses = TestResources.getExampleDiagnoses();
        List<DifferentialDiagnosis> orderedDiagnoses = refiner.getOrderedDiagnoses(originalDiagnoses, options);
        List<HpoDisease> diseases = refiner.getDiseases(orderedDiagnoses.stream().toList());
        Map<TermId, List<HpoFrequency>> hpoTermCounts = refiner.getHpoTermCounts(diseases);
        List<TermId> termIdsToRemove = Stream.of(sample.presentHpoTermIds(), sample.excludedHpoTermIds())
                .flatMap(Collection::stream).toList();
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = refiner.getMaxoToHpoTermIdMap(termIdsToRemove, hpoTermCounts);

        RefinementResults results = refiner.run(sample, orderedDiagnoses, options, null,
                                                maxoToHpoTermIdMap, hpoTermCounts, null);

        List<MaxodiffResult> resultsList = new ArrayList<>(results.maxodiffResults());
        resultsList.sort((a, b) -> b.maxoTermScore().scoreDiff().compareTo(a.maxoTermScore().scoreDiff()));
        String maxoId1 = resultsList.get(0).maxoTermScore().maxoId();
        String maxoId2 = resultsList.get(1).maxoTermScore().maxoId();

        assertEquals("MAXO:0010203", maxoId1);
        assertEquals("MAXO:0035050", maxoId2);

        TermId hpoId1 = resultsList.get(0).frequencies().get(0).hpoId();
        List<Float> hpoFreqs1 = resultsList.get(0).frequencies().get(0).frequencies();

        assertEquals("HP:0001650", hpoId1.getValue());
        assertEquals(1.0, hpoFreqs1.get(9).doubleValue());
        assertEquals(0.33, hpoFreqs1.get(11).doubleValue(), 1e-2);

    }

}