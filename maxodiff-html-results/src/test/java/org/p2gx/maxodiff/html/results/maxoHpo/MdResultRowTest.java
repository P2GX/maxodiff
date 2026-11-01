package org.p2gx.maxodiff.html.results.maxoHpo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MdResultRowTest {

    private static MdResultRow row(int initialRank, float averageRank) {
        return new MdResultRow("OMIM:154700", "Marfan syndrome", initialRank, averageRank, 20, List.of());
    }

    @Test
    void movingDownTheListIsADecline() {
        assertEquals(2.0f, row(1, 3.0f).getRankChange(), 1e-6f);
        assertEquals("Decline by 2.0", row(1, 3.0f).getRankChangeText());
    }

    @Test
    void movingUpTheListIsAnImprovement() {
        assertEquals(-13.0f, row(17, 4.0f).getRankChange(), 1e-6f);
        assertEquals("Improve by 13.0", row(17, 4.0f).getRankChangeText());
    }

    @Test
    void noMovementIsNoChange() {
        assertEquals("No change", row(5, 5.0f).getRankChangeText());
    }

    @Test
    void fractionalChangesShowOneDecimal() {
        assertEquals("Improve by 1.7", row(4, 2.3333f).getRankChangeText());
    }

    @Test
    void aSmallDeclineIsAShortBarToTheRight() {
        // 20 diseases: the largest possible change is 19; a change of +2 is 2/19 of the half track
        MdResultRow row = new MdResultRow("OMIM:154700", "Marfan syndrome", 1, 3.0f, 20, List.of());
        assertEquals("tornado-bar decline", row.getBarCssClass());
        assertEquals("left:50%;width:5.3%", row.getBarStyle());
    }

    @Test
    void anImprovementIsABarToTheLeft() {
        MdResultRow row = new MdResultRow("OMIM:301118", "Some syndrome", 17, 4.0f, 20, List.of());
        assertEquals("tornado-bar improve", row.getBarCssClass());
        assertEquals("right:50%;width:34.2%", row.getBarStyle());
    }

    @Test
    void noChangeHasNoLength() {
        MdResultRow row = new MdResultRow("OMIM:1", "X", 5, 5.0f, 20, List.of());
        assertEquals("tornado-bar none", row.getBarCssClass());
        assertEquals("left:50%;width:0.0%", row.getBarStyle());
    }

    @Test
    void aBarNeverGrowsBeyondItsHalfOfTheTrack() {
        MdResultRow row = new MdResultRow("OMIM:1", "X", 1, 90.0f, 20, List.of());   // more than the largest possible change
        assertEquals("left:50%;width:50.0%", row.getBarStyle());
    }

    @Test
    void aSingleDiseaseDoesNotCauseADivisionByZero() {
        MdResultRow row = new MdResultRow("OMIM:1", "X", 1, 1.0f, 1, List.of());
        assertEquals("left:50%;width:0.0%", row.getBarStyle());
    }
}