package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
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
 * This information can be used to identify the most probable HPO features
 * relevant to a specific intervention or to prioritize diseases in differential diagnosis.
 *
 * @author Martha Beckwith
 * @since 1.0
 */
public class MaxoHpoDiseaseRank {

    /** The ranked list of initial differential diagnoses produced by Phenomizer */
    private final List<DifferentialDiagnosis> initialDiagnoses;
    /** Object used to determine which phenotypes are ascertainable from a given sample. */
    private final AscertainablePhenotypes ascertainablePhenotypes;
    /** The set of all HPO term IDs that can be ascertained for this MAXO term. */
    private final Set<TermId> allMaxoAscertainedHpoIds;
    /** The MAXO term ID representing the diagnostic test being analyzed. */
    private final TermId maxoId;
    /** The MAXO label of the diagnostic test */
    private final String maxoLabel;

    /** List of counts of ascertainable HPO terms per disease. */
    private final List<Integer> ascertainedHpoCountList = new ArrayList<>();
    /** Mapping of HPO term IDs to lists of initial differential diagnosis disease rankings. */
    private final Map<TermId, List<Double>> hpoToRankMap = new HashMap<>();
    /** Mapping of HPO term IDs to normalized probabilities. */
    private Map<TermId, Double> hpoToProbabiltyMap = new HashMap<>();


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
                              AscertainablePhenotypes ascertainablePhenotypes,
                              Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                              TermId maxoId,
                              PpktSample sample,
                              int nDiagnoses,
                              String maxoLabel) {
        this.initialDiagnoses = initialDiagnoses;
        this.ascertainablePhenotypes = ascertainablePhenotypes;
        this.maxoId = maxoId;
        this.maxoLabel = maxoLabel;
        this.allMaxoAscertainedHpoIds = maxoToHpoTermIdMap.get(maxoId);
        makeAscertainedHpoCountListAndRankMap(sample, nDiagnoses);
        makeHpoToProbabilityMap(sample);
    }

    /**
     * Builds the list of ascertainable HPO counts per disease and populates
     * a mapping from HPO terms to disease ranks.
     * <p>
     * Each disease is assigned a rank inversely proportional to its position
     * in {@code initialDiagnoses}. HPO terms annotated to that disease are
     * then associated with this rank. {@code hpoToRankMap} will contain key: HPO TermId, value: List
     * of disease rank factors: 1, 1/2, 1/3, ..., 1/n that represent the ranks of the disease
     * that featured the HPO term. This means that each HPO term that is ascertainable by
     * at least one disease will have a List of factors such as [1/4, 1/17,...]. The sum
     * of these factors represents the overall probability that we will observe this
     * HPO if we perform the diagnostic modality represented by the MAxO term. We normalize
     * this to get a probability map for all such HPO terms. {@code ascertainedHpoCountList} will contain
     * a list of the counts of HPO terms per disease that we can ascertain with the
     * current MAxO term.  We sample from this list to get the number of simulated HPOs per iteration.
     *
     * @param sample        the sample (patient) whose ascertainable phenotypes are being analyzed
     * @param nDiagnoses    the number of top diagnoses to consider
     */
    public void makeAscertainedHpoCountListAndRankMap(PpktSample sample, int nDiagnoses) {

        for (DifferentialDiagnosis diagnosis : initialDiagnoses.subList(0, nDiagnoses)) {
            double diseaseRankFactor = 1.0 / (initialDiagnoses.indexOf(diagnosis) + 1);
            Set<TermId> diseaseAnnotatedHpoIds = ascertainablePhenotypes.getAscertainablePhenotypeIds(sample, diagnosis.diseaseId());
            // HpoToRankMap
            for (TermId hpoId : diseaseAnnotatedHpoIds) {
                hpoToRankMap.putIfAbsent(hpoId, new ArrayList<>());
                hpoToRankMap.get(hpoId).add(diseaseRankFactor);
            }
            // Maximum number of HPO terms for this disease that can be ascertained by this MAxO (not including
            // terms that are already observed/excluded in the Phenopacket).
            diseaseAnnotatedHpoIds.retainAll(allMaxoAscertainedHpoIds);
            ascertainedHpoCountList.add(diseaseAnnotatedHpoIds.size());

        }
    }

    /**
     * Constructs a normalized probability distribution over HPO terms based on
     * their cumulative disease ranks and presence in the MAXO ascertainable set.
     * <p>
     * HPO terms already observed or excluded in the {@code sample} are removed from
     * consideration. Terms with no associated rank receive a small prior probability.
     *
     * @param sample  the sample providing observed and excluded HPO terms
     */
    public void makeHpoToProbabilityMap(PpktSample sample) {

        Set<TermId> sampleTerms = sample.observedHpoTerms().stream().map(term -> TermId.of(term.termId())).collect(Collectors.toSet());
        sampleTerms.addAll(sample.excludedHpoTerms().stream().map(term -> TermId.of(term.termId())).collect(Collectors.toSet()));
        List<TermId> maxoAscertainedHpoIdsExclSample = new ArrayList<>(allMaxoAscertainedHpoIds);
        maxoAscertainedHpoIdsExclSample.removeAll(sampleTerms); // ascertainable terms  not in phenopacket

        Map<TermId, Double> hpoToProbabilityMapOriginal = new HashMap<>();
        for (TermId hpoId : maxoAscertainedHpoIdsExclSample) {
            Double EPSILON = 0.000001; // small probability
            hpoToProbabilityMapOriginal.put(hpoId, EPSILON);
        }
        for (Map.Entry<TermId, List<Double>> hpoRankMapEntry : hpoToRankMap.entrySet()) {
            TermId hpo = hpoRankMapEntry.getKey();
            List<Double> ranks = hpoRankMapEntry.getValue();
            double rankSum = ranks.stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
            hpoToProbabilityMapOriginal.replace(hpo, rankSum);
        }

        //Normalize probability values and order by decreasing probability
//        Double probabilitySum = hpoToProbabilityMapOriginal.values().stream().mapToDouble(Double::doubleValue).sum();
//        hpoToProbabilityMapOriginal.forEach( (id, prob) -> hpoToProbabilityMapOriginal.replace(id, prob/probabilitySum));

        hpoToProbabiltyMap = hpoToProbabilityMapOriginal.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b)->b, LinkedHashMap::new));

    }

    public List<Integer> getAscertainedHpoCountList() {
        return ascertainedHpoCountList;
    }

    public Map<TermId, Double> getHpoToProbabiltyMap() {
        return hpoToProbabiltyMap;
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

    public List<Integer> getSampledHpoCounts(int total) {

        List<Integer> result = new ArrayList<>();
        Map<Integer, Integer> freqMap = new HashMap<>();

        // Count how often each HPO count occurs
        for (Integer item : this.ascertainedHpoCountList) {
            freqMap.merge(item, 1, Integer::sum);
        }

        // Compute cumulative frequency list for sampling
        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>(freqMap.entrySet());
        List<Integer> cumulative = new ArrayList<>();
        int cumSum = 0;
        for (Map.Entry<Integer, Integer> e : entries) {
            cumSum += e.getValue();
            cumulative.add(cumSum);
        }


        // Draw "total" samples, weighted by frequency
        Random random = new Random();
        for (int i = 0; i < total; i++) {
            int r = random.nextInt(total); // random integer in [0, total)
            // Find which bin r falls into
            for (int j = 0; j < cumulative.size(); j++) {
                if (r < cumulative.get(j)) {
                    result.add(entries.get(j).getKey());
                }
            }
        }
        return result;
    }

    /**
     * Convenience class for clarity while constructing this object
     */
    public static class Builder {
        private List<DifferentialDiagnosis> initialDiagnoses;
        private AscertainablePhenotypes ascertainablePhenotypes;
        private Map<TermId, Set<TermId>> maxoToHpoTermIdMap;
        private TermId maxoId = null;
        private Integer nDiagnoses = null;
        private PpktSample sample;
        private String maxoLabel;


        public Builder initialDiagnoses(List<DifferentialDiagnosis> initialDiagnoses) {
            this.initialDiagnoses = initialDiagnoses;
            return this;
        }

        public Builder nDiagnoses(int n) {
            this.nDiagnoses = n;
            return this;
        }

        public Builder sample(PpktSample sample) {
            this.sample = sample;
            return this;
        }

        public Builder ascertainablePhenotypes(AscertainablePhenotypes ascertainablePhenotypes) {
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
