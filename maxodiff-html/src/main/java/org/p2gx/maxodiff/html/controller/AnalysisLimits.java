package org.p2gx.maxodiff.html.controller;

import java.util.ArrayList;
import java.util.List;

final class AnalysisLimits {

    static final int MIN_N_DISEASES = 20;
    static final int MAX_N_DISEASES = 40;
    static final int MAX_N_REPETITIONS = 100;

    private AnalysisLimits() {
    }

    /** @return the problems found, in plain language; empty if both values are acceptable */
    static List<UserFacingProblem> check(Integer nDiseases, Integer nRepetitions) {
        List<UserFacingProblem> problems = new ArrayList<>();
        if (nDiseases == null || nDiseases < MIN_N_DISEASES || nDiseases > MAX_N_DISEASES) {
            problems.add(new UserFacingProblem(
                    "Number of diseases out of range",
                    "You asked to compare " + (nDiseases == null ? "an unknown number of" : nDiseases)
                            + " candidate diagnoses, but this service supports " + MIN_N_DISEASES + " to "
                            + MAX_N_DISEASES + ". This number controls how many of the most likely diagnoses "
                            + "are followed while the effect of each test is simulated.",
                    "Please choose a value between " + MIN_N_DISEASES + " and " + MAX_N_DISEASES
                            + " from the list and run the analysis again."));
        }
        if (nRepetitions == null || nRepetitions < 1 || nRepetitions > MAX_N_REPETITIONS) {
            problems.add(new UserFacingProblem(
                    "Number of repetitions out of range",
                    "The analysis simulates the possible results of each test several times (repetitions). "
                            + "You asked for " + (nRepetitions == null ? "an unknown number of" : nRepetitions)
                            + ", but this service supports 1 to " + MAX_N_REPETITIONS + ". More repetitions "
                            + "make the result more stable but take longer.",
                    "Please choose a value from the list (for example 80 or 100) and run the analysis again."));
        }
        return problems;
    }
}