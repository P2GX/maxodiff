package org.monarchinitiative.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.monarchinitiative.maxodiff.core.TestResources;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class DiseaseProbabilityModelsTest {

    private final static List<DifferentialDiagnosis> DIFFERENTIAL_DIAGNOSES = TestResources.getExampleDiagnoses().stream().toList();

    private final static DiseaseProbabilityModels DISEASE_PROBABILITY_MODELS = new DiseaseProbabilityModels(DIFFERENTIAL_DIAGNOSES);

    private final static TermId TARGET_ID = TermId.of("OMIM:154700"); //rank 1 disease in example differential diagnoses

    /**
     * This tests calculating the disease probability using the ranked model.
     */
    @Test
    public void testRankedModel() {
        double rankedProbability = DISEASE_PROBABILITY_MODELS.ranked(TARGET_ID);
        assertEquals(0.09, rankedProbability, 1e-3);
     }



    public sealed interface TestOutcome {
        record OkRanked(double diseaseProbability) implements TestOutcome {}
        record OkSoftmax(double diseaseProbability) implements TestOutcome {}
        record OkExpDecay(double diseaseProbability) implements TestOutcome {}
        record Error(Supplier<? extends PhenolRuntimeException> exceptionSupplier) implements TestOutcome {}
    }

    public record TestIndividual(String description, TermId targetDiseaseId, TestOutcome expectedOutcome) {}

    /**
     *
     * @return Stream of individual test results for 4 tests: ranked, softmax, and exponential decay calculations
     * and no disease found.
     */
    private static Stream<TestIndividual> testGetIndividualDiseaseIds() {
        return Stream.of(
                new TestIndividual("ranked",
                        TARGET_ID,
                        new TestOutcome.OkRanked(0.09)),
                new TestIndividual("softmax",
                        TARGET_ID,
                        new TestOutcome.OkSoftmax(0.072)),
                new TestIndividual("exponential decay",
                        TARGET_ID,
                        new TestOutcome.OkExpDecay(0.632)),
                new TestIndividual("no disease",
                        TermId.of("OMIM:123456"),
                        new TestOutcome.Error(() -> new PhenolRuntimeException("Could not find disease id OMIM:123456 in differential diagnoses")))
        );
    }

    @ParameterizedTest
    @MethodSource("testGetIndividualDiseaseIds")
    void testEvaluateExpression(TestIndividual testCase) {
        TermId targetId = testCase.targetDiseaseId();
        switch (testCase.expectedOutcome()) {
            case TestOutcome.OkRanked(double expectedResult) ->
                    assertEquals(expectedResult, DISEASE_PROBABILITY_MODELS.ranked(targetId), 1.e-3,
                            "Incorrect evaluation for: " + testCase.description());
            case TestOutcome.OkSoftmax(double expectedResult) ->
                    assertEquals(expectedResult, DISEASE_PROBABILITY_MODELS.softmax(targetId), 1.e-3,
                            "Incorrect evaluation for: " + testCase.description());
            case TestOutcome.OkExpDecay(double expectedResult) ->
                    assertEquals(expectedResult, DISEASE_PROBABILITY_MODELS.exponentialDecay(targetId, 1), 1.e-3,
                            "Incorrect evaluation for: " + testCase.description());
            case TestOutcome.Error(Supplier<? extends RuntimeException> exceptionSupplier) ->
                    assertThrows(exceptionSupplier.get().getClass(),
                            () -> DISEASE_PROBABILITY_MODELS.ranked(TermId.of("OMIM:123456")),
                            "Incorrect error handling for: " + testCase.description());
        }

    }
}
