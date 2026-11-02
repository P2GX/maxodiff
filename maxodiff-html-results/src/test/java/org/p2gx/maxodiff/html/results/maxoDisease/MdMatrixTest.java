package org.p2gx.maxodiff.html.results.maxoDisease;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;
import org.p2gx.maxodiff.core.analysis.RankedOmimTerm;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MdMatrixTest {

    private static final SimpleTerm MARFAN = new SimpleTerm(TermId.of("OMIM:154700"), "Marfan syndrome");
    private static final SimpleTerm LDS4 = new SimpleTerm(TermId.of("OMIM:616914"), "Loeys-Dietz syndrome 4");

    private static RankedMaxoResult result(int n, List<RankedOmimTerm> diseases) {
        return new RankedMaxoResult(new SimpleTerm(TermId.of(String.format("MAXO:%07d", n)), "Test " + n),
                5.0, diseases, List.of(), List.of());
    }

    @Test
    void longDiseaseNamesAreCutAtTwentyFiveCharactersWithAnEllipsis() {
        assertThat(MdMatrix.shorten("a".repeat(25), 25)).isEqualTo("a".repeat(25));
        assertThat(MdMatrix.shorten("a".repeat(26), 25)).isEqualTo("a".repeat(24) + "\u2026").hasSize(25);
    }

    @Test
    void columnsAreTheDiseasesInInitialRankOrderAndRowsAreTheTests() {
        MdMatrix matrix = MdMatrix.of(List.of(
                result(1, List.of(new RankedOmimTerm(LDS4, 2, 1.0f), new RankedOmimTerm(MARFAN, 1, 3.0f))),
                result(2, List.of(new RankedOmimTerm(MARFAN, 1, 1.0f), new RankedOmimTerm(LDS4, 2, 2.0f)))));

        assertThat(matrix.getColumns()).extracting(MdMatrix.Column::label)
                .containsExactly("Marfan syndrome", "Loeys-Dietz syndrome 4");
        assertThat(matrix.getRows()).extracting(MdMatrix.Row::maxoLabel).containsExactly("Test 1", "Test 2");
    }

    @Test
    void cellsAreMatchedByDiseaseIdEvenWhenTheOrderDiffersBetweenTests() {
        // the second test lists the diseases in the opposite order
        MdMatrix matrix = MdMatrix.of(List.of(
                result(1, List.of(new RankedOmimTerm(MARFAN, 1, 3.0f), new RankedOmimTerm(LDS4, 2, 1.0f))),
                result(2, List.of(new RankedOmimTerm(LDS4, 2, 1.0f), new RankedOmimTerm(MARFAN, 1, 3.0f)))));

        assertThat(matrix.getRows().get(0).cells()).extracting(MdMatrix.Cell::text).containsExactly("+2.0", "−1.0");
        assertThat(matrix.getRows().get(1).cells()).extracting(MdMatrix.Cell::text).containsExactly("+2.0", "−1.0");
    }

    @Test
    void aDiseaseThatDoesNotMoveHasAnEmptyCell() {
        MdMatrix matrix = MdMatrix.of(List.of(result(1, List.of(new RankedOmimTerm(MARFAN, 1, 1.0f)))));

        MdMatrix.Cell cell = matrix.getRows().getFirst().cells().getFirst();
        assertThat(cell.isEmpty()).isTrue();
        assertThat(cell.style()).isEmpty();
    }

    @Test
    void movingUpIsBlueAndMovingDownIsRed() {
        MdMatrix matrix = MdMatrix.of(List.of(
                result(1, List.of(new RankedOmimTerm(MARFAN, 1, 3.0f), new RankedOmimTerm(LDS4, 2, 1.0f)))));

        List<MdMatrix.Cell> cells = matrix.getRows().getFirst().cells();
        assertThat(cells.get(0).style()).contains("255,69,96");   // Marfan declines
        assertThat(cells.get(1).style()).contains("0,143,251");   // Loeys-Dietz improves
    }

    @Test
    void withoutResultsThereAreNoRowsAndNoColumns() {
        MdMatrix matrix = MdMatrix.of(List.of());

        assertThat(matrix.getRows()).isEmpty();
        assertThat(matrix.getColumns()).isEmpty();
    }

}