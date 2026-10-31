package org.p2gx.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The score is the sum, over the diseases of the initial ranking that also appear in the new ranking, of
 * |newRank - initialRank| / initialRank. Ranks are 1-based positions in the list.
 */
class ValidationWeightedRankDiffTest {

    private static final double DELTA = 1e-9;

    private static final TermId D1 = TermId.of("OMIM:100001");
    private static final TermId D2 = TermId.of("OMIM:100002");
    private static final TermId D3 = TermId.of("OMIM:100003");
    private static final TermId D4 = TermId.of("OMIM:100004");

    /** A ranking in the given order (the scores themselves play no part in the calculation). */
    private static List<DifferentialDiagnosis> ranking(TermId... ids) {
        return java.util.Arrays.stream(ids).map(id -> DifferentialDiagnosis.of(id, 1.0, 1.0)).toList();
    }

    private static double score(List<DifferentialDiagnosis> initial, List<DifferentialDiagnosis> afterTest) {
        return ValidationModel.weightedRankDiff(initial, afterTest).validationScore();
    }

    @Test
    void identicalRankingsScoreZero() {
        assertEquals(0.0, score(ranking(D1, D2, D3), ranking(D1, D2, D3)), DELTA);
    }

    @Test
    void swappingTheTopTwoScoresOnePointFive() {
        // D1: 1 -> 2 gives |2-1|/1 = 1.0;  D2: 2 -> 1 gives |1-2|/2 = 0.5
        assertEquals(1.5, score(ranking(D1, D2), ranking(D2, D1)), DELTA);
    }

    @Test
    void aMoveAtTheTopCountsMoreThanTheSameMoveLowerDown() {
        // swap ranks 1 and 2: 1.0 + 0.5
        assertEquals(1.5, score(ranking(D1, D2, D3, D4), ranking(D2, D1, D3, D4)), DELTA);
        // swap ranks 3 and 4: 1/3 + 1/4
        assertEquals(1.0 / 3 + 1.0 / 4, score(ranking(D1, D2, D3, D4), ranking(D1, D2, D4, D3)), DELTA);
    }

    @Test
    void aDiseaseMissingFromTheNewRankingIsIgnored() {
        // D3 is not in the new ranking and adds nothing; D1: 1 -> 2 gives 1.0, D2: 2 -> 1 gives 0.5
        assertEquals(1.5, score(ranking(D1, D2, D3), ranking(D2, D1)), DELTA);
    }

    @Test
    void aDiseaseOnlyInTheNewRankingIsIgnoredButStillShiftsTheOthers() {
        // D3 is new at the top: D1 goes 1 -> 2 (1.0), D2 goes 2 -> 3 (0.5)
        assertEquals(1.5, score(ranking(D1, D2), ranking(D3, D1, D2)), DELTA);
    }

    @Test
    void reversingThreeDiseases() {
        // D1: 1 -> 3 gives 2.0; D2: unchanged; D3: 3 -> 1 gives 2/3
        assertEquals(2.0 + 2.0 / 3, score(ranking(D1, D2, D3), ranking(D3, D2, D1)), DELTA);
    }

    @Test
    void theArgumentOrderMatters() {
        List<DifferentialDiagnosis> a = ranking(D1, D2, D3);
        List<DifferentialDiagnosis> b = ranking(D2, D3, D1);
        // (a, b): D1 1->3 = 2.0, D2 2->1 = 0.5, D3 3->2 = 1/3
        assertEquals(2.0 + 0.5 + 1.0 / 3, score(a, b), DELTA);
        // (b, a): D2 1->2 = 1.0, D3 2->3 = 0.5, D1 3->1 = 2/3
        assertEquals(1.0 + 0.5 + 2.0 / 3, score(b, a), DELTA);
        assertNotEquals(score(a, b), score(b, a), DELTA);
    }

    @Test
    void emptyRankingsScoreZero() {
        assertEquals(0.0, score(List.of(), ranking(D1, D2)), DELTA);
        assertEquals(0.0, score(ranking(D1, D2), List.of()), DELTA);
        assertEquals(0.0, score(List.of(), List.of()), DELTA);
    }
}