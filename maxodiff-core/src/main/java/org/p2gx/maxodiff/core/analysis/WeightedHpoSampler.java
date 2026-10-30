package org.p2gx.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Draws HPO terms at random, with probability proportional to their weight,
 * without replacement (each term can be picked at most once per call).
 * <p>
 * The weights are relative, not probabilities: they need not sum to 1. In this application
 * a weight is the sum of {@code 1/rank} over the top candidate diseases that feature the term,
 * so a term supported by the first-ranked disease has weight of at least 1. The sampler is
 * therefore deliberately written so that it does <i>not</i> assume weights lie in [0, 1]:
 * each draw picks a uniform number in [0, total remaining weight) and takes the term whose
 * cumulative weight range contains it. This is equivalent to normalizing the weights first.
 * <p>
 * Terms supported by highly ranked diseases are favored, but not exclusively. For weights
 * 1.0, 0.5 and 0.1, the first draw yields each term with probability 62.5%, 31.3% and 6.3%
 * respectively.
 * <p>
 * Terms with a tiny weight (such as the EPSILON in MaxoHpoDiseaseRank prior given to terms that the test can reveal but
 * no top disease has) remain possible but are very unlikely to be drawn.
 */
public final class WeightedHpoSampler {

    private final List<HpoWeight> weights;

    public WeightedHpoSampler(List<HpoWeight> weights) {
        this.weights = List.copyOf(weights);
        Set<TermId> seen = new HashSet<>();
        for (HpoWeight w : this.weights) {
            if (!seen.add(w.hpoId())) {
                throw new IllegalArgumentException("Duplicate HPO term: " + w.hpoId());
            }
        }
    }

    /** @return the number of terms that can be sampled from */
    public int size() {
        return weights.size();
    }

    /**
     * @param k      number of distinct terms to draw, 0 <= k <= {@link #size()}
     * @param random source of randomness (pass a seeded instance for reproducible results)
     * @return {@code k} distinct term IDs; at each draw, each remaining term is chosen with
     *         probability weight / (sum of remaining weights)
     */
    public List<TermId> sample(int k, Random random) {
        if (k < 0 || k > weights.size()) {
            throw new IllegalArgumentException("k must be between 0 and " + weights.size() + " but was " + k);
        }
        List<HpoWeight> remaining = new ArrayList<>(weights);
        double total = remaining.stream().mapToDouble(HpoWeight::weight).sum();
        List<TermId> selected = new ArrayList<>(k);

        for (int n = 0; n < k; n++) {
            double r = random.nextDouble() * total;   // uniform in [0, total): no normalization needed
            int pick = remaining.size() - 1;          // fallback guards against rounding at the upper end
            double cumulative = 0.0;
            for (int i = 0; i < remaining.size(); i++) {
                cumulative += remaining.get(i).weight();
                if (r < cumulative) {
                    pick = i;
                    break;
                }
            }
            HpoWeight chosen = remaining.remove(pick);
            total -= chosen.weight();
            selected.add(chosen.hpoId());
        }
        return selected;
    }
}