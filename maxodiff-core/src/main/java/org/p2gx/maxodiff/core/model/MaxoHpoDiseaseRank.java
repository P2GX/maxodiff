package org.p2gx.maxodiff.core.model;


import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.HpoWeight;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

/**
 * The {@code MaxoHpoDiseaseRank} class computes relationships between
 * Medical Action Ontology (MAXO) terms and Human Phenotype Ontology (HPO)
 * terms within a differential diagnosis context.
 * <p>
 * For a given MAXO term (representing a medical intervention), this class:
 * <ul>
 *   <li>Evaluates how many of the disease’s phenotypic features are ascertainable via that MAXO term.</li>
 *   <li>Constructs rank and probability mappings between HPO terms and diseases.</li>
 *   <li>Provides a probability distribution over HPO terms inferred from observed disease rankings.</li>
 * </ul>
 * <p>
 * The question being modeled is:
 * if we perform diagnostic test M, what new phenotypes might we observe in this patient?
 * To answer it we look at the top n candidate diseases. Disease #1 is the most likely, so it gets weight 1.
 * Disease #2 gets 1/2, and so on.
 *
 * @author Martha Beckwith
 * @since 1.0
 */
public class MaxoHpoDiseaseRank {

    /** The ranked list of initial differential diagnoses produced by Phenomizer */
    private final List<DifferentialDiagnosis> initialDiagnoses;
    /** The set of all HPO term IDs that can be ascertained for this MAXO term. */
    private final Set<TermId> allMaxoAscertainedHpoIds;
    /** The MAXO term ID representing the diagnostic test being analyzed. */
    private final TermId maxoId;
    /** The MAXO label of the diagnostic test */
    private final String maxoLabel;

    /** One entry per top disease, in differential-diagnosis order. */
    private final List<DiseaseYield> yields;
    /** Every term the test can reveal that is not yet in the sample, with its weight, highest first. */
    private final List<HpoWeight> hpoWeights;
    /** Small prior weight for terms the test can reveal but no top disease has.
     * The value of EPSILON is a modelling choice;  with 30–200 detectable terms 1e-3 gives roughly 1–3% of draws from
     * outside the top diseases’ terms. */
    static final double EPSILON = 1e-3;

    /**
     * Construct a {@code MaxoHpoDiseaseRank} instance.
     *
     * @param initialDiagnoses        the initial Phenomizer-prioritized list of {@link DifferentialDiagnosis}
     * @param ascertainablePhenotypes helper for retrieving ascertainable phenotypes for a given disease and sample
     * @param maxoToHpoTermIdMap      map of MAXO term IDs to the corresponding sets of HPO term IDs they can ascertain
     * @param maxoId                  the MAXO term being evaluated
     * @param maxoLabel               the MAXO term label
     */
    public MaxoHpoDiseaseRank(List<DifferentialDiagnosis> initialDiagnoses,
                              AssessablePhenotypes ascertainablePhenotypes,
                              Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                              TermId maxoId,
                              PhenopacketData sample,
                              int nDiagnoses,
                              String maxoLabel) {
        Set<TermId> maxoHpoIds = maxoToHpoTermIdMap.get(maxoId);
        if (maxoHpoIds == null) {
            throw new IllegalArgumentException(
                    String.format("Unknown MAxO id %s: no detectable HPO terms are mapped to it", maxoId));
        }
        if (nDiagnoses < 0 || nDiagnoses > initialDiagnoses.size()) {
            throw new IllegalArgumentException(
                    String.format("nDiagnoses must be between 0 and %d but was %d", initialDiagnoses.size(), nDiagnoses));
        }
        this.allMaxoAscertainedHpoIds = maxoHpoIds;
        this.initialDiagnoses = initialDiagnoses;
        this.maxoId = maxoId;
        this.maxoLabel = maxoLabel;
        this.yields = IntStream.range(0, nDiagnoses)
                        .mapToObj(i -> {
                            DifferentialDiagnosis dx = initialDiagnoses.get(i);
                            return DiseaseYield.of(
                                    dx,
                                    i+1,
                                    ascertainablePhenotypes.getAssessablePhenotypeIds(sample, dx.diseaseId()),
                                    allMaxoAscertainedHpoIds);
                        })
                        .toList();
        this.hpoWeights = buildHpoWeights(sample);
    }

    /** Sum, per HPO term, the weights of the top diseases that the test could reveal it in. */
    private Map<TermId, Double> sumWeights() {
        Map<TermId, Double> weights = new HashMap<>();
        for (DiseaseYield y : yields) {
            for (TermId hpoId : y.revealedByMaxo()) {
                weights.merge(hpoId, y.weight(), Double::sum);
            }
        }
        return weights;
    }

    private List<HpoWeight> buildHpoWeights(PhenopacketData sample) {
        Set<TermId> sampleTerms = sample.observed().stream().map(SimpleTerm::tid).collect(Collectors.toSet());
        sample.excluded().forEach(t -> sampleTerms.add(t.tid()));
        Map<TermId, Double> weights = sumWeights();

        return allMaxoAscertainedHpoIds.stream()
                .filter(id -> !sampleTerms.contains(id))
                .map(id -> new HpoWeight(id, weights.getOrDefault(id, EPSILON)))
                .sorted(Comparator.comparingDouble(HpoWeight::weight).reversed()
                        .thenComparing(w -> w.hpoId().getValue()))   // deterministic tie-break
                .toList();
    }

    public List<HpoWeight> getHpoWeights() {
        return hpoWeights;
    }
    public List<Integer> getAscertainedHpoCountList() {
        return yields.stream().map(DiseaseYield::count).toList();
    }

    public TermId getMaxoId() {
        return maxoId;
    }

    public String getMaxoLabel() {
        return maxoLabel;
    }

    public List<DifferentialDiagnosis> getInitialDiagnoses() {
        return initialDiagnoses;
    }

    public Set<TermId> getAllMaxoAscertainedHpoIds() {return allMaxoAscertainedHpoIds;}


    public List<Integer> getSampledHpoCounts(int total) {
        return getSampledHpoCounts(total, new Random());
    }

    public List<Integer> getSampledHpoCounts(int total, Random random) {
        List<Integer> counts = getAscertainedHpoCountList();
        if (counts.isEmpty()) {
            throw new IllegalStateException("No diagnoses to sample from");
        }
        return random.ints(total, 0, counts.size()).mapToObj(counts::get).toList();
    }

    /**
     * Convenience class for clarity while constructing this object
     */
    public static class Builder {
        private List<DifferentialDiagnosis> initialDiagnoses;
        private AssessablePhenotypes ascertainablePhenotypes;
        private Map<TermId, Set<TermId>> maxoToHpoTermIdMap;
        private TermId maxoId = null;
        private Integer nDiagnoses = null;
        private PhenopacketData sample;
        private String maxoLabel;


        public Builder initialDiagnoses(List<DifferentialDiagnosis> initialDiagnoses) {
            this.initialDiagnoses = initialDiagnoses;
            return this;
        }

        public Builder nDiagnoses(int n) {
            this.nDiagnoses = n;
            return this;
        }

        public Builder sample(PhenopacketData sample) {
            this.sample = sample;
            return this;
        }

        public Builder ascertainablePhenotypes(AssessablePhenotypes ascertainablePhenotypes) {
            this.ascertainablePhenotypes = ascertainablePhenotypes;
            return this;
        }

        public Builder maxoToHpoTermIdMap(Map<TermId, Set<TermId>> maxoToHpoTermIdMap) {
            this.maxoToHpoTermIdMap = maxoToHpoTermIdMap;
            return this;
        }

        public Builder maxoId(TermId maxoId) {
            this.maxoId = maxoId;
            return this;
        }

        public Builder maxoLabel(String maxoLabel) {
            this.maxoLabel = maxoLabel;
            return this;
        }

        /** @return a new {@link Builder} instance for {@link MaxoHpoDiseaseRank} */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builds a {@link MaxoHpoDiseaseRank} instance after validating all required fields.
         *
         * @return new {@link MaxoHpoDiseaseRank} instance
         * @throws NullPointerException if any required field is missing
         */
        public MaxoHpoDiseaseRank build() {
            Objects.requireNonNull(initialDiagnoses, "initialDiagnoses must not be null");
            Objects.requireNonNull(ascertainablePhenotypes, "ascertainablePhenotypes must not be null");
            Objects.requireNonNull(maxoToHpoTermIdMap, "maxoToHpoTermIdMap must not be null");
            Objects.requireNonNull(maxoId, "maxoId must not be null");
            Objects.requireNonNull(maxoLabel, "maxoLabel must not be null");
            Objects.requireNonNull(nDiagnoses, "nDiagnoses must not be null");
            Objects.requireNonNull(sample, "sample must not be null");
            return new MaxoHpoDiseaseRank(this.initialDiagnoses,
                    this.ascertainablePhenotypes,
                    this.maxoToHpoTermIdMap,
                    this.maxoId,
                    this.sample,
                    this.nDiagnoses,
                    this.maxoLabel);
        }
    }
}
