package org.p2gx.maxodiff.html.results.maxoHpo;

import org.junit.jupiter.api.Test;
import org.p2gx.maxodiff.html.results.maxoHpo.RankScale;
import org.p2gx.maxodiff.html.results.maxoHpo.ScaleTick;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankScaleTest {

    private static List<String> labels(int nDiseases) {
        return RankScale.ticks(nDiseases).stream().map(ScaleTick::label).toList();
    }

    @Test
    void twentyDiseasesGiveTicksEveryFivePositions() {
        assertEquals(List.of("\u221215", "\u221210", "\u22125", "0", "+5", "+10", "+15"), labels(20));
    }

    @Test
    void fortyDiseasesGiveTicksEveryTenPositions() {
        assertEquals(List.of("\u221230", "\u221220", "\u221210", "0", "+10", "+20", "+30"), labels(40));
    }

    @Test
    void aSmallListGetsATickForEveryPosition() {
        assertEquals(List.of("\u22122", "\u22121", "0", "+1", "+2"), labels(3));
    }

    @Test
    void zeroIsInTheMiddleAndTheOthersAreSymmetric() {
        List<ScaleTick> ticks = RankScale.ticks(20);
        assertEquals("left:10.5%", ticks.get(0).style());     // -15 of at most 19
        assertEquals("left:50.0%", ticks.get(3).style());     // 0
        assertEquals("left:89.5%", ticks.get(6).style());     // +15
    }

    @Test
    void aSingleDiseaseDoesNotFail() {
        assertEquals(List.of("\u22121", "0", "+1"), labels(1));
    }
}