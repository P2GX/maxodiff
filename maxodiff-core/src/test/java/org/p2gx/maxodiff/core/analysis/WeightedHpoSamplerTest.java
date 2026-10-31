package org.p2gx.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WeightedHpoSamplerTest {

    private static final TermId A = TermId.of("HP:0000001");
    private static final TermId B = TermId.of("HP:0000002");
    private static final TermId C = TermId.of("HP:0000003");

    private static WeightedHpoSampler sampler(double wa, double wb, double wc) {
        return new WeightedHpoSampler(List.of(new HpoWeight(A, wa), new HpoWeight(B, wb), new HpoWeight(C, wc)));
    }

    @Test
    void returnsExactlyKDistinctTerms() {
        List<TermId> result = sampler(1, 1, 1).sample(2, new Random(1));
        assertEquals(2, result.size());
        assertEquals(2, new HashSet<>(result).size());
    }

    @Test
    void zeroDrawsGivesEmptyList() {
        assertTrue(sampler(1, 1, 1).sample(0, new Random(1)).isEmpty());
    }

    @Test
    void drawingAllTermsReturnsEveryTermOnce() {
        Set<TermId> result = new HashSet<>(sampler(5, 1, 0.001).sample(3, new Random(7)));
        assertEquals(Set.of(A, B, C), result);
    }

    @Test
    void askingForMoreTermsThanExistThrows() {
        assertThrows(IllegalArgumentException.class, () -> sampler(1, 1, 1).sample(4, new Random(1)));
    }

    @Test
    void negativeKThrows() {
        assertThrows(IllegalArgumentException.class, () -> sampler(1, 1, 1).sample(-1, new Random(1)));
    }

    @Test
    void sameSeedGivesSameResult() {
        WeightedHpoSampler s = sampler(3, 2, 1);
        assertEquals(s.sample(2, new Random(42)), s.sample(2, new Random(42)));
    }

    @Test
    void singleDrawIsProportionalToWeight() {
        // Weights are deliberately NOT normalised (9 + 1 = 10): the old code was skewed by exactly this.
        WeightedHpoSampler s = new WeightedHpoSampler(List.of(new HpoWeight(A, 9), new HpoWeight(B, 1)));
        Random random = new Random(42);
        int draws = 10_000;
        int aCount = 0;
        for (int i = 0; i < draws; i++) {
            if (s.sample(1, random).getFirst().equals(A)) {
                aCount++;
            }
        }
        // expected 9000, standard deviation ~30; the bounds are about 10 sigma wide
        assertTrue(aCount > 8_700 && aCount < 9_300, "A was drawn " + aCount + " times");
    }

    @Test
    void lowWeightTermsAreStillReachable() {
        WeightedHpoSampler s = new WeightedHpoSampler(List.of(new HpoWeight(A, 1), new HpoWeight(B, 1)));
        Random random = new Random(3);
        Set<TermId> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            seen.addAll(s.sample(1, random));
        }
        assertEquals(Set.of(A, B), seen);
    }

    @Test
    void duplicateTermsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new WeightedHpoSampler(List.of(new HpoWeight(A, 1), new HpoWeight(A, 2))));
    }

    @Test
    void emptySamplerOnlyAllowsZeroDraws() {
        WeightedHpoSampler s = new WeightedHpoSampler(List.of());
        assertTrue(s.sample(0, new Random(1)).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> s.sample(1, new Random(1)));
    }
}