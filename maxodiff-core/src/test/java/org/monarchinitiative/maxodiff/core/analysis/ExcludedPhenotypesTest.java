package org.monarchinitiative.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.TestResources;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.model.SamplePhenopacket;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ExcludedPhenotypesTest {

     private final static HpoDiseases hpoDiseases = TestResources.hpoDiseases();
     private final static Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap = TestResources.hpoToMaxoToy();

     private final static ExcludedPhenotypes excludedPhenotypes = new ExcludedPhenotypes(hpoToMaxoTermMap);

    /**
     * This tests if the right disease is obtained from HpoDiseases, and if it has the correct HPO TermIds.
     */
     @Test
     public void testFindDisease() {
         //Sanity check: can we get the right disease from HpoDiseases, and does it have the right HPO Term Ids?
         TermId diseaseId = TermId.of("OMIM:620365");
         Optional<HpoDisease> returnedDiseaseIdOpt = hpoDiseases.diseaseById(diseaseId);
         assertTrue(returnedDiseaseIdOpt.isPresent(), "Did not find OMIM:620365");
         var disease = returnedDiseaseIdOpt.get();
         List<TermId> termIdList = disease.annotationTermIdList();
         assertEquals(13, termIdList.size());
         assertTrue(termIdList.contains(TermId.of("HP:0006739")), "Did not find HP:0006739");
         assertTrue(termIdList.contains(TermId.of("HP:0002863")), "Did not find HP:0002863");
         assertTrue(termIdList.contains(TermId.of("HP:0100651")), "Did not find HP:0100651");
     }

    /**
     *
     * @return Sample phenopacket with one included HPO term Id and one disease Id.
     */
    public static Sample getPPkt1() {
        List<TermId> presentTerms = List.of(
                TermId.of("HP:0006739")
        );
        List<TermId> excludedTerms = List.of();
        List<TermId> diseaseIds = List.of(TermId.of("OMIM:620365"));

        return Sample.of("sample1", presentTerms, excludedTerms);//, diseaseIds);
    }

    /**
     *
     * @return Sample phenopacket with two included HPO term Ids and one disease Ids.
     */
    public static Sample getPPkt2() {
        List<TermId> presentTerms = List.of(
                TermId.of("HP:0006739"),
                TermId.of("HP:0002863")
        );
        List<TermId> excludedTerms = List.of();
        List<TermId> diseaseIds = List.of(TermId.of("OMIM:620365"));

        return Sample.of("sample1", presentTerms, excludedTerms);//, diseaseIds);
    }

    //TODO: write edge case test for inheritance, e.g. if maxo term has both hpo Id and it's parent associated w/ it

    /**
     * This tests getting the excluded phenotypes for the sample phenopacket with one included HPO term.
     */
    @Test
    public void testExcludedPhenotypes1() {
         // Get excluded phenotypes given phenopacket
         Sample s1 = getPPkt1();
         Set<TermId> excludedPhenotypeIds = excludedPhenotypes.getExcludedPhenotypes(s1);

         // HPO term in phenopacket can be ascertained by 2 Maxo terms (MAXO:0000671 and MAXO:0000691)
         // 4 total HPO terms can ascertained by both Maxo terms in toy example
         // 1 exists in phenopacket, so 3 excluded
         assertEquals(3, excludedPhenotypeIds.size());
     }

    public sealed interface TestOutcome {
        record Ok(int nExcludedTerms) implements TestOutcome {}
        record Error(Supplier<? extends PhenolRuntimeException> exceptionSupplier) implements TestOutcome {}
    }

    public record TestIndividual(String description, Sample myPPkt, TestOutcome expectedOutcome) {}

    /**
     *
     * @return Stream of individual test results for 2 tests: excluded phenotypes for ppkt with 1 HPO term,
     * and excluded phenotypes for ppkt with 2 HPO terms.
     */
    private static Stream<TestIndividual> testGetIndividualDiseaseIds() {
        return Stream.of(
                new TestIndividual("46 year old female, infantile onset (1 term)",
                        getPPkt1(),
                        new TestOutcome.Ok(3)),
                new TestIndividual("46 year old female, infantile onset (2 terms)",
                        getPPkt2(),
                        new TestOutcome.Ok(3))//,
//                new TestIndividual("No disease id",
//                        getPPktEmptyDisease(),
//                        new TestOutcome.Error(() -> new PhenolRuntimeException("No disease id found")))
        );
    }

    @ParameterizedTest
    @MethodSource("testGetIndividualDiseaseIds")
    void testEvaluateExpression(TestIndividual testCase) {
        Sample ppkti = testCase.myPPkt();
//        TermId targetId = ppkti.diseaseIds().get(0);
        switch (testCase.expectedOutcome()) {
            case TestOutcome.Ok(int expectedResult) ->
                    assertEquals(expectedResult, excludedPhenotypes.getExcludedPhenotypes(ppkti).size(),
                            "Incorrect evaluation for: " + testCase.description());
            case TestOutcome.Error(Supplier<? extends RuntimeException> exceptionSupplier) ->
                    assertThrows(exceptionSupplier.get().getClass(),
                            () -> excludedPhenotypes.getExcludedPhenotypes(ppkti),
                            "Incorrect error handling for: " + testCase.description());
        }
    }
}
