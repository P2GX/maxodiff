package org.monarchinitiative.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.monarchinitiative.maxodiff.core.TestResources;
import org.monarchinitiative.maxodiff.core.model.SamplePhenopacket;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class PotentialPhenotypesTest {

     private final static HpoDiseases hpoDiseases = TestResources.hpoDiseases();

     private final static PotentialPhenotypes potentialPhenotypes = new PotentialPhenotypes(hpoDiseases);

     @Test
     public void testFindDisease() {
         //Sanity check: can we get the right disease from HpoDiseases, and does it have the right HPO Term Ids?
         TermId diseaseId = TermId.of("OMIM:615837");
         Optional<HpoDisease> returnedDiseaseIdOpt = hpoDiseases.diseaseById(diseaseId);
         assertTrue(returnedDiseaseIdOpt.isPresent(), "Did not find OMIM:615837");
         var disease = returnedDiseaseIdOpt.get();
         List<TermId> termIdList = disease.annotationTermIdList();
         assertEquals(3, termIdList.size());
         assertTrue(termIdList.contains(TermId.of("HP:0008619")), "Did not find HP:0008619");
         assertTrue(termIdList.contains(TermId.of("HP:0001751")), "Did not find HP:0001751");
         assertTrue(termIdList.contains(TermId.of("HP:0000505")), "Did not find HP:0000505");
     }

    public static SamplePhenopacket getPPkt1() {
        List<TermId> presentTerms = List.of(
                TermId.of("HP:0008619")
        );
        List<TermId> excludedTerms = List.of();
        List<TermId> diseaseIds = List.of(TermId.of("OMIM:615837"));

        return new SamplePhenopacket("sample1", presentTerms, excludedTerms, diseaseIds);
    }

    public static SamplePhenopacket getPPkt2() {
        List<TermId> presentTerms = List.of(
                TermId.of("HP:0008619"),
                TermId.of("HP:0001751")
        );
        List<TermId> excludedTerms = List.of();
        List<TermId> diseaseIds = List.of(TermId.of("OMIM:615837"));

        return new SamplePhenopacket("sample1", presentTerms, excludedTerms, diseaseIds);
    }

    public static SamplePhenopacket getPPktEmptyDisease() {
        List<TermId> presentTerms = List.of(
                TermId.of("HP:0008619")
        );
        List<TermId> excludedTerms = List.of();
        List<TermId> diseaseIds = List.of();

        return new SamplePhenopacket("sample2", presentTerms, excludedTerms, diseaseIds);
    }

    @Test
    public void testPotentialPhenotypes1() {
         // Get potential phenotypes given phenopacket
         SamplePhenopacket s1 = getPPkt1();
         Set<TermId> potentialPhenotypeIds = potentialPhenotypes.getPotentialPhenotypeIds(s1);
         // Disease associated with ppkt has 3 phenotype terms, example ppkt already has 1, so expect 2 here
        assertEquals(2, potentialPhenotypeIds.size());
     }

    public sealed interface TestOutcome {
        record Ok(Set<TermId> termIdSet) implements TestOutcome {}
        record Error(Supplier<? extends PhenolRuntimeException> exceptionSupplier) implements TestOutcome {}
    }

    public record TestIndividual(String description, SamplePhenopacket myPPkt, TestOutcome expectedOutcome) {}

    private static Stream<TestIndividual> testGetIndividualDiseaseIds() {
        return Stream.of(
                new TestIndividual("46 year old female, infantile onset (1 term)",
                        getPPkt1(),
                        new TestOutcome.Ok(Set.of(TermId.of("HP:0001751"), TermId.of("HP:0000505")))),
                new TestIndividual("46 year old female, infantile onset (2 terms)",
                        getPPkt2(),
                        new TestOutcome.Ok(Set.of(TermId.of("HP:0000505")))),
                new TestIndividual("No disease id",
                        getPPktEmptyDisease(),
                        new TestOutcome.Error(() -> new PhenolRuntimeException("No disease id found")))
        );
    }

    @ParameterizedTest
    @MethodSource("testGetIndividualDiseaseIds")
    void testEvaluateExpression(TestIndividual testCase) {
        SamplePhenopacket ppkti = testCase.myPPkt();
        switch (testCase.expectedOutcome()) {
            case TestOutcome.Ok(Set<TermId> expectedResult) ->
                    assertEquals(expectedResult, potentialPhenotypes.getPotentialPhenotypeIds(ppkti),
                            "Incorrect evaluation for: " + testCase.description());
            case TestOutcome.Error(Supplier<? extends RuntimeException> exceptionSupplier) ->
                    assertThrows(exceptionSupplier.get().getClass(),
                            () -> potentialPhenotypes.getPotentialPhenotypeIds(ppkti),
                            "Incorrect error handling for: " + testCase.description());
        }
    }
}
