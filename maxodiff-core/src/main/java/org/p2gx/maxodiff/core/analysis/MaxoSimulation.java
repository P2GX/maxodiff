package org.p2gx.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;
import org.p2gx.maxodiff.core.model.PhenopacketData;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Simulates what would happen to a patient's differential diagnosis if a MAxO diagnostic test were performed.
 * <p>
 * In each repetition a number of HPO terms is drawn (weighted by how strongly the top candidate diseases
 * support them) and added to a copy of the <i>original</i> patient. The differential diagnosis is then re-run
 * and compared with the initial one. Repetitions are independent: findings from one repetition are never
 * carried into the next.
 */
public final class MaxoSimulation {

    private final WeightedHpoSampler sampler;
    private final PhenopacketData ppkt;
    private final DDxEngine engine;
    private final Set<TermId> diseaseIds;
    private final List<DifferentialDiagnosis> initialDiagnoses;
    private final Function<TermId, String> hpoLabel;

    public MaxoSimulation(WeightedHpoSampler sampler,
                          PhenopacketData ppkt,
                          DDxEngine engine,
                          Set<TermId> diseaseIds,
                          List<DifferentialDiagnosis> initialDiagnoses,
                          Function<TermId, String> hpoLabel) {
        this.sampler = Objects.requireNonNull(sampler);
        this.ppkt = Objects.requireNonNull(ppkt);
        this.engine = Objects.requireNonNull(engine);
        this.diseaseIds = Objects.requireNonNull(diseaseIds);
        this.initialDiagnoses = Objects.requireNonNull(initialDiagnoses);
        this.hpoLabel = Objects.requireNonNull(hpoLabel);
    }

    /**
     * @param nHposPerRepetition how many HPO terms to draw in each repetition; its size is the number of
     *                           repetitions. A value larger than the number of available terms is reduced to it.
     * @param random             source of randomness
     */
    public SimulationOutcome run(List<Integer> nHposPerRepetition, Random random) {
        List<List<DifferentialDiagnosis>> newDiagnoses = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        Map<SimpleTerm, Integer> selectionCounts = new LinkedHashMap<>();

        for (int nHpos : nHposPerRepetition) {
            Set<SimpleTerm> simulated = sampler.sample(Math.min(nHpos, sampler.size()), random).stream()
                    .map(id -> new SimpleTerm(id, hpoLabel.apply(id)))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            simulated.forEach(t -> selectionCounts.merge(t, 1, Integer::sum));

            PhenopacketData newSample = ppkt.withAdditionalObserved(simulated);
            List<DifferentialDiagnosis> diagnoses = engine.run(newSample, diseaseIds);
            newDiagnoses.add(diagnoses);
            scores.add(ValidationModel.weightedRankDiff(initialDiagnoses, diagnoses).validationScore());
        }
        return new SimulationOutcome(newDiagnoses, scores, selectionCounts);
    }
}