package org.p2gx.maxodiff.html.controller;

import org.phenopackets.schema.v2.Phenopacket;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class AnalysisLimits {

    static final int MIN_N_DISEASES = 20;
    static final int MAX_N_DISEASES = 40;
    static final int MAX_N_REPETITIONS = 100;

    private static final Pattern TARGET_DISEASE = Pattern.compile("(OMIM|ORPHA|DECIPHER):[0-9A-Za-z]+");

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
                    "Please choose a value between 1 and " + MAX_N_REPETITIONS + " and run the analysis again."));
        }
        return problems;
    }

    /** @return a problem if the phenopacket is missing or has no id; the patient is identified by that id */
    static List<UserFacingProblem> checkPhenopacket(Phenopacket ppkt) {
        if (ppkt == null || ppkt.getId().isBlank()) {
            return List.of(new UserFacingProblem(
                    "The phenopacket has no id",
                    "maxodiff identifies the patient by the id of the phenopacket, and this one has none.",
                    "Add an \"id\" to the phenopacket and send it again."));
        }
        return List.of();
    }

    /** @return a problem if the target disease is not an OMIM, ORPHA or DECIPHER identifier such as OMIM:157700 */
    static List<UserFacingProblem> checkTargetDisease(String targetDiseaseId) {
        if (targetDiseaseId == null || !TARGET_DISEASE.matcher(targetDiseaseId).matches()) {
            return List.of(new UserFacingProblem(
                    "The target disease is not valid",
                    "The target disease must be given as an OMIM, ORPHA or DECIPHER identifier, but it was \""
                            + targetDiseaseId + "\".",
                    "Use an identifier such as OMIM:157700."));
        }
        return List.of();
    }
}