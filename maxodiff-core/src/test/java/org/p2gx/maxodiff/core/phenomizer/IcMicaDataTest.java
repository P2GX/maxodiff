package org.p2gx.maxodiff.core.phenomizer;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.HTMLFrequencyMap;
import org.p2gx.maxodiff.core.model.TermPair;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IcMicaDataTest {

    private static final double DELTA = 1e-9;

    private static TermPair pair(int a, int b) {
        return TermPair.symmetric(TermId.of(String.format("HP:%07d", a)), TermId.of(String.format("HP:%07d", b)));
    }

    private static IcMicaData dataWith(double... values) {
        Map<TermPair, Double> dict = new HashMap<>();
        for (int i = 0; i < values.length; i++) {
            dict.put(pair(i + 1, i + 100), values[i]);
        }
        return new IcMicaData(dict, null);
    }

    @Test
    void maxIcIsTheLargestValue() {
        assertEquals(3.5, dataWith(1.0, 3.5, 2.0).maxIc(), DELTA);
    }

    @Test
    void infiniteValuesAreIgnored() {
        assertEquals(2.0, dataWith(1.0, Double.POSITIVE_INFINITY, 2.0).maxIc(), DELTA);
    }

    @Test
    void anEmptyDictionaryGivesTheDefault() {
        assertEquals(IcMicaData.DEFAULT_MAX_IC, dataWith().maxIc(), DELTA);
    }

    @Test
    void aDictionaryWithOnlyInfiniteValuesGivesTheDefault() {
        assertEquals(IcMicaData.DEFAULT_MAX_IC, dataWith(Double.POSITIVE_INFINITY).maxIc(), DELTA);
    }

    @Test
    void theFrequencyMapReportsTheStoredMaximum() {
        HTMLFrequencyMap map = new HTMLFrequencyMap(null, dataWith(1.0, 7.25, 2.0)); // diseases are not needed here
        assertEquals(7.25, map.maxMica(), DELTA);
    }
}