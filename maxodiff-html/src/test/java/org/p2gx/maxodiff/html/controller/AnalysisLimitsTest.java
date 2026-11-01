package org.p2gx.maxodiff.html.controller;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisLimitsTest {

    @Test
    void valuesOfferedByTheFormGiveNoProblems() {
        assertTrue(AnalysisLimits.check(20, 100).isEmpty());
        assertTrue(AnalysisLimits.check(40, 20).isEmpty());
    }

    @Test
    void theLimitsThemselvesAreAccepted() {
        assertTrue(AnalysisLimits.check(AnalysisLimits.MIN_N_DISEASES, 1).isEmpty());
        assertTrue(AnalysisLimits.check(AnalysisLimits.MAX_N_DISEASES, AnalysisLimits.MAX_N_REPETITIONS).isEmpty());
    }

    @Test
    void anOutOfRangeNumberOfDiseasesIsExplainedWithTheValueAndTheAllowedRange() {
        List<UserFacingProblem> problems = AnalysisLimits.check(5000, 100);

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).explanation().contains("5000"));
        assertTrue(problems.get(0).action().contains("between 20 and 40"));
    }

    @Test
    void bothProblemsAreReportedTogether() {
        assertEquals(2, AnalysisLimits.check(0, 0).size());
    }

    @Test
    void zeroNegativeAndMissingRepetitionsAreProblems() {
        assertEquals(1, AnalysisLimits.check(20, 0).size());
        assertEquals(1, AnalysisLimits.check(20, -5).size());
        assertEquals(1, AnalysisLimits.check(20, null).size());
    }
}