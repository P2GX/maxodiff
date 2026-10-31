package org.p2gx.maxodiff.core.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;
import org.p2gx.maxodiff.core.model.PhenopacketData;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MaxoSimulationTest {

    private static final TermId D1 = TermId.of("OMIM:100001");
    private static final TermId D2 = TermId.of("OMIM:100002");
    private static final TermId P_OBS = TermId.of("HP:0000100");   // already observed in the patient
    private static final TermId A = TermId.of("HP:0000001");
    private static final TermId B = TermId.of("HP:0000002");
    private static final TermId C = TermId.of("HP:0000003");
    private static final TermId E = TermId.of("HP:0000004");

    /** Records every sample it is asked to analyse and returns a fixed differential diagnosis. */
    private static final class StubEngine implements DDxEngine {
        final List<PhenopacketData> samples = new ArrayList<>();
        final List<Collection<TermId>> targets = new ArrayList<>();
        private final List<DifferentialDiagnosis> answer;

        StubEngine(List<DifferentialDiagnosis> answer) {
            this.answer = answer;
        }

        @Override
        public List<DifferentialDiagnosis> run(PhenopacketData sample) {
            return run(sample, null);
        }

        @Override
        public List<DifferentialDiagnosis> run(PhenopacketData sample, Collection<TermId> targetDiseases) {
            samples.add(sample);
            targets.add(targetDiseases);
            return answer;
        }
    }

    private List<DifferentialDiagnosis> initial;
    private PhenopacketData ppkt;
    private StubEngine engine;
    private Set<TermId> diseaseIds;

    @BeforeEach
    void setUp() {
        initial = List.of(DifferentialDiagnosis.of(D1, 3.0, 1.0), DifferentialDiagnosis.of(D2, 2.0, 1.0));
        List<DifferentialDiagnosis> afterTest =
                List.of(DifferentialDiagnosis.of(D2, 4.0, 1.0), DifferentialDiagnosis.of(D1, 1.0, 1.0));
        ppkt = new PhenopacketData("sample1", List.of(new SimpleTerm(P_OBS, "observed")),
                List.of(), List.of(D1), List.of(), false);
        engine = new StubEngine(afterTest);
        diseaseIds = Set.of(D1, D2);
    }

    private static WeightedHpoSampler sampler(TermId... ids) {
        return new WeightedHpoSampler(Arrays.stream(ids).map(id -> new HpoWeight(id, 1.0)).toList());
    }

    private MaxoSimulation simulation(WeightedHpoSampler sampler) {
        return new MaxoSimulation(sampler, ppkt, engine, diseaseIds, initial, id -> "label-" + id.getValue());
    }

    private static List<TermId> observedIds(PhenopacketData p) {
        return p.observed().stream().map(SimpleTerm::tid).toList();
    }

    @Test
    void engineIsRunOncePerRepetitionOnTheRequestedDiseases() {
        SimulationOutcome outcome = simulation(sampler(A, B, C)).run(List.of(1, 1, 1, 1), new Random(1));

        assertEquals(4, engine.samples.size());
        engine.targets.forEach(t -> assertEquals(diseaseIds, t));
        assertEquals(4, outcome.newDiagnoses().size());
        assertEquals(4, outcome.scores().size());
    }

    @Test
    void eachRepetitionStartsFromTheOriginalPatient() {
        // Draws of 1, 2 and 1 terms: if findings carried over, later samples would keep growing.
        simulation(sampler(A, B, C, E)).run(List.of(1, 2, 1), new Random(1));

        assertEquals(2, observedIds(engine.samples.get(0)).size()); // 1 original + 1 drawn
        assertEquals(3, observedIds(engine.samples.get(1)).size()); // 1 original + 2 drawn
        assertEquals(2, observedIds(engine.samples.get(2)).size()); // back to 1 original + 1 drawn
        engine.samples.forEach(s -> assertTrue(observedIds(s).contains(P_OBS)));
    }

    @Test
    void selectionCountsAddUpToTheTermsDrawn() {
        SimulationOutcome outcome = simulation(sampler(A, B, C)).run(List.of(1, 1, 1, 2), new Random(5));

        int total = outcome.selectionCounts().values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(5, total); // 1+1+1+2; the old cumulative loop gave a larger number
    }

    @Test
    void requestingMoreTermsThanExistIsCappedNotAnError() {
        SimulationOutcome outcome = simulation(sampler(A, B)).run(List.of(5), new Random(1));

        assertEquals(3, observedIds(engine.samples.getFirst()).size()); // original + both available terms
        assertEquals(2, outcome.selectionCounts().size());
    }
}