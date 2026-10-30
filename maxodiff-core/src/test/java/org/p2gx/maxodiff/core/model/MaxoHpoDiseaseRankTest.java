package org.p2gx.maxodiff.core.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.HpoWeight;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Scenario used throughout (three candidate diseases, one MAXO test):
 * <pre>
 *  disease  rank  weight  assessable terms (not already in patient)
 *  D1       1     1       A, B, Z
 *  D2       2     1/2     B, C
 *  D3       3     1/3     A
 *
 *  MAXO test can detect: A, B, C, E, S, X
 *  Patient: observed S, excluded X       (so S and X must never be offered)
 *  Z is not detectable by the test; E is detectable but no disease has it.
 * </pre>
 * Expected weights: B = 1 + 1/2, A = 1 + 1/3, C = 1/2, E = EPSILON.
 */
class MaxoHpoDiseaseRankTest {

    private static final double DELTA = 1e-9;

    private static final TermId MAXO = TermId.of("MAXO:0000001");
    private static final String MAXO_LABEL = "test procedure";

    private static final TermId D1 = TermId.of("OMIM:100001");
    private static final TermId D2 = TermId.of("OMIM:100002");
    private static final TermId D3 = TermId.of("OMIM:100003");

    private static final TermId A = TermId.of("HP:0000001");
    private static final TermId B = TermId.of("HP:0000002");
    private static final TermId C = TermId.of("HP:0000003");
    private static final TermId E = TermId.of("HP:0000005"); // detectable, no disease has it
    private static final TermId S = TermId.of("HP:0000006"); // observed in patient
    private static final TermId X = TermId.of("HP:0000007"); // excluded in patient
    private static final TermId Z = TermId.of("HP:0000009"); // disease has it, test cannot detect it

    private List<DifferentialDiagnosis> diagnoses;
    private Map<TermId, Set<TermId>> maxoToHpo;
    private PhenopacketData sample;
    private StubAssessablePhenotypes assessable;

    /** Returns canned answers instead of consulting HpoDiseases, and records which diseases were asked about. */
    private static final class StubAssessablePhenotypes extends AssessablePhenotypes {
        private final Map<TermId, Set<TermId>> byDisease;
        final List<TermId> queried = new ArrayList<>();

        StubAssessablePhenotypes(Map<TermId, Set<TermId>> byDisease) {
            super(null); // the parent only stores this, and the overridden method never uses it
            this.byDisease = byDisease;
        }

        @Override
        public Set<TermId> getAssessablePhenotypeIds(PhenopacketData ppkt, TermId diseaseId) {
            queried.add(diseaseId);
            return byDisease.get(diseaseId);
        }
    }

    @BeforeEach
    void setUp() {
        diagnoses = List.of(
                DifferentialDiagnosis.of(D1, 3.0, 1.0),
                DifferentialDiagnosis.of(D2, 2.0, 1.0),
                DifferentialDiagnosis.of(D3, 1.0, 1.0));
        maxoToHpo = Map.of(MAXO, Set.of(A, B, C, E, S, X));
        sample = new PhenopacketData("sample1",
                List.of(new SimpleTerm(S, "observed")),
                List.of(new SimpleTerm(X, "excluded")),
                List.of(), List.of(), false);

        // AssessablePhenotypes needs real HpoDiseases, so we stub what it would return.
        // The real class already removes the patient's terms, so the stubs do too.
        assessable = new StubAssessablePhenotypes(Map.of(
                D1, Set.of(A, B, Z),
                D2, Set.of(B, C),
                D3, Set.of(A)));
    }

    private MaxoHpoDiseaseRank build(int nDiagnoses) {
        if (nDiagnoses < 1) {
            throw new IllegalArgumentException("Number of diagnoses ust be at least 1");
        }
        return MaxoHpoDiseaseRank.Builder.builder()
                .initialDiagnoses(diagnoses)
                .ascertainablePhenotypes(assessable)
                .maxoToHpoTermIdMap(maxoToHpo)
                .maxoId(MAXO)
                .maxoLabel(MAXO_LABEL)
                .sample(sample)
                .nDiagnoses(nDiagnoses)
                .build();
    }

    private static Map<TermId, Double> asMap(List<HpoWeight> weights) {
        return weights.stream().collect(Collectors.toMap(HpoWeight::hpoId, HpoWeight::weight));
    }

    // ---- weights ----------------------------------------------------------------

    @Test
    void weightIsSumOfReciprocalRanksOfDiseasesFeaturingTheTerm() {
        Map<TermId, Double> w = asMap(build(3).getHpoWeights());

        assertEquals(1.0 + 1.0 / 2, w.get(B), DELTA); // D1 and D2
        assertEquals(1.0 + 1.0 / 3, w.get(A), DELTA); // D1 and D3
        assertEquals(1.0 / 2, w.get(C), DELTA);       // D2 only
    }

    @Test
    void weightsAreOrderedHighestFirst() {
        List<TermId> order = build(3).getHpoWeights().stream().map(HpoWeight::hpoId).toList();
        assertEquals(List.of(B, A, C, E), order);
    }

    @Test
    void detectableTermNoDiseaseHasGetsEpsilon() {
        Map<TermId, Double> w = asMap(build(3).getHpoWeights());
        assertEquals(MaxoHpoDiseaseRank.EPSILON, w.get(E), DELTA);
    }

    @Test
    void termsAlreadyObservedOrExcludedInPatientAreNotOffered() {
        Set<TermId> offered = asMap(build(3).getHpoWeights()).keySet();
        assertFalse(offered.contains(S), "observed term must be absent");
        assertFalse(offered.contains(X), "excluded term must be absent");
    }

    @Test
    void termsTheTestCannotDetectAreNotOffered() {
        assertFalse(asMap(build(3).getHpoWeights()).containsKey(Z));
    }

    @Test
    void onlyTheTopNDiagnosesContribute() {
        MaxoHpoDiseaseRank rank = build(1);
        Map<TermId, Double> w = asMap(rank.getHpoWeights());

        assertEquals(1.0, w.get(A), DELTA);
        assertEquals(1.0, w.get(B), DELTA);
        assertEquals(MaxoHpoDiseaseRank.EPSILON, w.get(C), DELTA); // only D2 has C, and D2 is not considered
        assertEquals(List.of(D1), assessable.queried, "Only the top diagnosis should have been consulted");
    }

    @Test
    void weightsListIsEveryDetectableTermNotInPatient() {
        assertEquals(Set.of(A, B, C, E), asMap(build(3).getHpoWeights()).keySet());
    }

    // ---- counts -----------------------------------------------------------------

    @Test
    void countListHoldsRevealableTermsPerDiseaseInDiagnosisOrder() {
        // D1: A,B (Z not detectable) = 2; D2: B,C = 2; D3: A = 1
        assertEquals(List.of(2, 2, 1), build(3).getAscertainedHpoCountList());
    }

    @Test
    void countListOnlyCoversTheTopNDiagnoses() {
        assertEquals(List.of(2), build(1).getAscertainedHpoCountList());
    }

    // ---- sampling the counts ----------------------------------------------------

    @Test
    void sampledCountsHaveExactlyTheRequestedSizeEvenWhenLargerThanDiagnosisCount() {
        // 1000 draws from 3 diagnoses: the old implementation returned far fewer than 1000
        assertEquals(1000, build(3).getSampledHpoCounts(1000, new Random(1)).size());
    }

    @Test
    void sampledCountsOnlyContainValuesFromTheCountList() {
        Set<Integer> seen = new HashSet<>(build(3).getSampledHpoCounts(500, new Random(2)));
        assertTrue(Set.of(1, 2).containsAll(seen), "unexpected values: " + seen);
    }

    @Test
    void sampledCountsFollowTheFrequencyOfTheCountList() {
        // counts are [2, 2, 1]: expect about two thirds 2s and one third 1s
        List<Integer> sampled = build(3).getSampledHpoCounts(30_000, new Random(42));
        long twos = sampled.stream().filter(n -> n == 2).count();
        double fraction = twos / (double) sampled.size();
        assertEquals(2.0 / 3, fraction, 0.02);
    }

    @Test
    void sampledCountsAreReproducibleWithTheSameSeed() {
        MaxoHpoDiseaseRank rank = build(3);
        assertEquals(rank.getSampledHpoCounts(50, new Random(7)), rank.getSampledHpoCounts(50, new Random(7)));
    }

    @Test
    void zeroSamplesGivesEmptyList() {
        assertTrue(build(3).getSampledHpoCounts(0, new Random(1)).isEmpty());
    }


    // ---- simple getters ---------------------------------------------------------

    @Test
    void gettersReturnWhatWasSupplied() {
        MaxoHpoDiseaseRank rank = build(3);
        assertEquals(MAXO, rank.getMaxoId());
        assertEquals(MAXO_LABEL, rank.getMaxoLabel());
        assertEquals(diagnoses, rank.getInitialDiagnoses());
        assertEquals(Set.of(A, B, C, E, S, X), rank.getAllMaxoAscertainedHpoIds());
    }

    // ---- builder validation -----------------------------------------------------

    @Test
    void builderRejectsEachMissingField() {
        assertThrows(NullPointerException.class, () -> validBuilder().initialDiagnoses(null).build());
        assertThrows(NullPointerException.class, () -> validBuilder().ascertainablePhenotypes(null).build());
        assertThrows(NullPointerException.class, () -> validBuilder().maxoToHpoTermIdMap(null).build());
        assertThrows(NullPointerException.class, () -> validBuilder().maxoId(null).build());
        assertThrows(NullPointerException.class, () -> validBuilder().maxoLabel(null).build());
        assertThrows(NullPointerException.class, () -> validBuilder().sample(null).build());
    }

    @Test
    void builderRejectsMissingNDiagnoses() {
        MaxoHpoDiseaseRank.Builder b = MaxoHpoDiseaseRank.Builder.builder()
                .initialDiagnoses(diagnoses).ascertainablePhenotypes(assessable)
                .maxoToHpoTermIdMap(maxoToHpo).maxoId(MAXO).maxoLabel(MAXO_LABEL).sample(sample);
        NullPointerException e = assertThrows(NullPointerException.class, b::build);
        assertEquals("nDiagnoses must not be null", e.getMessage());
    }

    private MaxoHpoDiseaseRank.Builder validBuilder() {
        return MaxoHpoDiseaseRank.Builder.builder()
                .initialDiagnoses(diagnoses).ascertainablePhenotypes(assessable)
                .maxoToHpoTermIdMap(maxoToHpo).maxoId(MAXO).maxoLabel(MAXO_LABEL)
                .sample(sample).nDiagnoses(3);
    }

    // ---- guard clauses (FAIL until you add them) --------------------------------

    @Test
    void moreDiagnosesRequestedThanAvailableIsRejected() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> build(4));
    }

    @Test
    void negativeNDiagnosesIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> build(-1));
    }
}