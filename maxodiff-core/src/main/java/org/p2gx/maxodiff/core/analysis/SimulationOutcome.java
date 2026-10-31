package org.p2gx.maxodiff.core.analysis;

import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the repeated simulation of one MAxO test produced (see MaxoTermEvaluator).
 *
 * @param newDiagnoses    the differential diagnosis for each simulated patient, one entry per repetition
 * @param scores          the validation score for each repetition, same order
 * @param selectionCounts for each HPO term drawn at least once, the number of repetitions in which it was drawn
 */
public record SimulationOutcome(List<List<DifferentialDiagnosis>> newDiagnoses,
                                List<Double> scores,
                                Map<SimpleTerm, Integer> selectionCounts) {

    public SimulationOutcome {
        newDiagnoses = List.copyOf(newDiagnoses);
        scores = List.copyOf(scores);
        selectionCounts = Collections.unmodifiableMap(new LinkedHashMap<>(selectionCounts));
    }

    /** @return the mean score over all repetitions, or 0.0 if there were none */
    public double meanScore() {
        return scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
}