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
}