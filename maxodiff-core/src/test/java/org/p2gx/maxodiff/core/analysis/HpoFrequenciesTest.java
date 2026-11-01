package org.p2gx.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.TestResources;
import org.p2gx.maxodiff.core.model.TermPair;
import org.p2gx.maxodiff.core.phenomizer.IcMicaData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HpoFrequenciesTest {

    private static final HpoDiseases DISEASES = TestResources.hpoDiseases();

    /** A made-up but deterministic dictionary, covering all pairs among the annotations of the first few diseases. */
    private static IcMicaData syntheticIcData() {
        Map<TermPair, Double> dict = new HashMap<>();
        DISEASES.stream().limit(5).forEach(disease -> {
            List<TermId> ids = disease.annotationTermIdList();
            for (TermId a : ids) {
                for (TermId b : ids) {
                    dict.put(TermPair.symmetric(a, b), (Math.abs(a.hashCode() + b.hashCode()) % 20) / 2.0);
                }
            }
        });
        return new IcMicaData(dict, null);
    }

    @Test
    void thereIsOneRecordForEachAnnotatedTermOfEachDisease() {
        long expected = DISEASES.stream().mapToLong(d -> d.annotationTermIdList().size()).sum();
        assertEquals(expected, HpoFrequencies.compute(DISEASES, syntheticIcData()).size());
    }

    @Test
    void micaValuesMatchTheOriginalPerPairCalculation() {
        IcMicaData icData = syntheticIcData();
        HTMLFrequencyMap reference = new HTMLFrequencyMap(DISEASES, icData);

        // the first diseases are the ones the dictionary has values for; the reference method is slow, so check a sample
        HpoFrequencies.compute(DISEASES, icData).stream().limit(300).forEach(f ->
                assertEquals(reference.micaForDisease(f.hpoId(), f.diseaseId()), f.mica(), 1e-6f,
                        f.diseaseId() + " / " + f.hpoId()));
    }

    @Test
    void frequenciesAreBetweenZeroAndOne() {
        HpoFrequencies.compute(DISEASES, syntheticIcData()).stream().limit(2000).forEach(f -> {
            assertTrue(f.frequency() >= 0f && f.frequency() <= 1f, "frequency " + f.frequency());
        });
    }
}