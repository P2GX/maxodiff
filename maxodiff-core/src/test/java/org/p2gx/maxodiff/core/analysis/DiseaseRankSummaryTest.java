package org.p2gx.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiseaseRankSummaryTest {

    private static final float DELTA = 1e-6f;

    private static final TermId D1 = TermId.of("OMIM:100001");
    private static final TermId D2 = TermId.of("OMIM:100002");
    private static final TermId D3 = TermId.of("OMIM:100003");

    private static final SimpleTerm DISEASE_1 = new SimpleTerm(D1, "disease 1");

    private static List<DifferentialDiagnosis> ranking(TermId... ids) {
        return Arrays.stream(ids).map(id -> DifferentialDiagnosis.of(id, 1.0, 1.0)).toList();
    }

    // ---- rankOf ----

    @Test
    void rankIsTheOneBasedPosition() {
        List<DifferentialDiagnosis> r = ranking(D1, D2, D3);
        assertEquals(1, DiseaseRankSummary.rankOf(r, D1));
        assertEquals(3, DiseaseRankSummary.rankOf(r, D3));
    }

    @Test
    void rankOfAnAbsentDiseaseIsZero() {
        assertEquals(0, DiseaseRankSummary.rankOf(ranking(D1, D2), D3));
        assertEquals(0, DiseaseRankSummary.rankOf(List.of(), D1));
    }

    // ---- summarize ----

    @Test
    void initialRankComesFromTheInitialDiagnosis() {
        RankedOmimTerm t = DiseaseRankSummary.summarize(DISEASE_1, ranking(D2, D3, D1), List.of(ranking(D1)));
        assertEquals(3, t.initialRank());
    }

    @Test
    void theDiseaseIsPassedThrough() {
        RankedOmimTerm t = DiseaseRankSummary.summarize(DISEASE_1, ranking(D1), List.of(ranking(D1)));
        assertEquals(DISEASE_1, t.omimTerm());
    }

    @Test
    void oneRepetitionGivesThatRank() {
        RankedOmimTerm t = DiseaseRankSummary.summarize(DISEASE_1, ranking(D1, D2), List.of(ranking(D2, D1)));
        assertEquals(2.0f, t.averageRank(), DELTA);
    }

    @Test
    void averageOfWholeRanksIsExact() {
        // D1 is ranked 3, 1 and 2 in the three repetitions: average 2
        RankedOmimTerm t = DiseaseRankSummary.summarize(DISEASE_1, ranking(D1, D2, D3),
                List.of(ranking(D2, D3, D1), ranking(D1, D2, D3), ranking(D2, D1, D3)));
        assertEquals(2.0f, t.averageRank(), DELTA);
    }

    @Test
    void averageKeepsItsFractionalPart() {
        // D1 is ranked 2 and 1: the average is 1.5
        RankedOmimTerm t = DiseaseRankSummary.summarize(DISEASE_1, ranking(D1, D2),
                List.of(ranking(D2, D1), ranking(D1, D2)));
        assertEquals(1.5f, t.averageRank(), DELTA);
    }

    @Test
    void averageOfThreeRepetitionsIsNotRounded() {
        // D1 is ranked 1, 2 and 2: the average is 5/3
        RankedOmimTerm t = DiseaseRankSummary.summarize(DISEASE_1, ranking(D1, D2),
                List.of(ranking(D1, D2), ranking(D2, D1), ranking(D2, D1)));
        assertEquals(5.0f / 3, t.averageRank(), DELTA);
    }

    @Test
    void repetitionsWhereTheDiseaseWasNotRankedAreLeftOutOfTheAverage() {
        // ranked 1 in the first repetition, absent from the second: average of [1] is 1
        RankedOmimTerm t = DiseaseRankSummary.summarize(DISEASE_1, ranking(D1, D2),
                List.of(ranking(D1, D2), ranking(D2)));
        assertEquals(1.0f, t.averageRank(), DELTA);
    }

    @Test
    void aDiseaseNeverRankedGetsAverageZero() {
        // Assumption: this never happens in practice. If it did, 0 would look like a rank better than first.
        RankedOmimTerm t = DiseaseRankSummary.summarize(DISEASE_1, ranking(D1, D2),
                List.of(ranking(D2), ranking(D2, D3)));
        assertEquals(0.0f, t.averageRank(), DELTA);
    }

    @Test
    void noRepetitionsGivesAverageZero() {
        RankedOmimTerm t = DiseaseRankSummary.summarize(DISEASE_1, ranking(D1), List.of());
        assertEquals(0.0f, t.averageRank(), DELTA);
    }

    @Test
    void aDiseaseMissingFromTheInitialDiagnosisHasInitialRankZero() {
        RankedOmimTerm t = DiseaseRankSummary.summarize(DISEASE_1, ranking(D2, D3), List.of(ranking(D1)));
        assertEquals(0, t.initialRank());
    }

    @Test
    void signedRankChangeIsNegativeWhenTheDiseaseMovesUp() {
        // initial rank 3, rank 1 in the only repetition
        RankedOmimTerm t = DiseaseRankSummary.summarize(DISEASE_1, ranking(D2, D3, D1), List.of(ranking(D1, D2, D3)));
        assertEquals(-2.0f, t.signedRankChange(), DELTA);
    }
}