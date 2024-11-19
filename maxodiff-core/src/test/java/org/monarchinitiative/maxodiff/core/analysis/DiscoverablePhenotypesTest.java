package org.monarchinitiative.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.TestResources;
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

public class DiscoverablePhenotypesTest {

     private final static HpoDiseases hpoDiseases = TestResources.hpoDiseases();
     private final static Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap = TestResources.hpoToMaxo();

     private final static PotentialPhenotypes potentialPhenotypes = new PotentialPhenotypes(hpoDiseases);
     private final static ExcludedPhenotypes excludedPhenotypes = new ExcludedPhenotypes(hpoDiseases, hpoToMaxoTermMap);

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

    public static SamplePhenopacket getPPkt1() {
        List<TermId> presentTerms = List.of(
                TermId.of("HP:0006739")
        );
        List<TermId> excludedTerms = List.of();
        List<TermId> diseaseIds = List.of(TermId.of("OMIM:620365"));

        return new SamplePhenopacket("sample1", presentTerms, excludedTerms, diseaseIds);
    }

    public static SamplePhenopacket getPPkt2() {
        List<TermId> presentTerms = List.of(
                TermId.of("HP:0006739"),
                TermId.of("HP:0002863")
        );
        List<TermId> excludedTerms = List.of();
        List<TermId> diseaseIds = List.of(TermId.of("OMIM:620365"));

        return new SamplePhenopacket("sample1", presentTerms, excludedTerms, diseaseIds);
    }

    public static SamplePhenopacket getPPktEmptyDisease() {
        List<TermId> presentTerms = List.of(
                TermId.of("HP:0006739")
        );
        List<TermId> excludedTerms = List.of();
        List<TermId> diseaseIds = List.of();

        return new SamplePhenopacket("sample2", presentTerms, excludedTerms, diseaseIds);
    }

    @Test
    public void testDiscoverablePhenotypes1() {
         // Get excluded phenotypes given phenopacket
         SamplePhenopacket s1 = getPPkt1();
         TermId targetId = s1.diseaseIds().get(0);
         Set<TermId> potentialPhenotypeIds = potentialPhenotypes.getPotentialPhenotypeIds(s1, targetId);
         Set<TermId> excludedPhenotypeIds = excludedPhenotypes.getExcludedPhenotypeIds(s1, targetId);
         Set<TermId> discoverablePhenotypeIds = DiscoverablePhenotypes.getDiscoverablePhenotypeIds(
                potentialPhenotypeIds,
                excludedPhenotypeIds);
//         System.out.println(potentialPhenotypeIds);
//         System.out.println(excludedPhenotypeIds);
//         System.out.println(discoverablePhenotypeIds);
         // HPO term in phenopacket can be ascertained by 2 Maxo terms (MAXO:0000671 and MAXO:0000691)
         // 67 total HPO terms can ascertained by both Maxo terms
         assertEquals(10, discoverablePhenotypeIds.size());
     }

    public sealed interface TestOutcome {
        record Ok(int nExludedTerms) implements TestOutcome {}
//        record Error(Supplier<? extends PhenolRuntimeException> exceptionSupplier) implements TestOutcome {}
    }

    public record TestIndividual(String description, SamplePhenopacket myPPkt, TestOutcome expectedOutcome) {}

    private static Stream<TestIndividual> testGetIndividualDiseaseIds() {
        return Stream.of(
                new TestIndividual("46 year old female, infantile onset (1 term)",
                        getPPkt1(),
                        new TestOutcome.Ok(10)),
                new TestIndividual("46 year old female, infantile onset (2 terms)",
                        getPPkt2(),
                        new TestOutcome.Ok(10))
        );
    }

    @ParameterizedTest
    @MethodSource("testGetIndividualDiseaseIds")
    void testEvaluateExpression(TestIndividual testCase) {
        SamplePhenopacket ppkti = testCase.myPPkt();
        TermId targetId = ppkti.diseaseIds().get(0);
        Set<TermId> potentialPhenotypeIds = potentialPhenotypes.getPotentialPhenotypeIds(ppkti, targetId);
        Set<TermId> excludedPhenotypeIds = excludedPhenotypes.getExcludedPhenotypeIds(ppkti, targetId);
        Set<TermId> discoverablePhenotypeIds = DiscoverablePhenotypes.getDiscoverablePhenotypeIds(
                potentialPhenotypeIds,
                excludedPhenotypeIds);

//        System.out.println(potentialPhenotypeIds);
//        System.out.println(excludedPhenotypeIds);
//        System.out.println(discoverablePhenotypeIds);

        switch (testCase.expectedOutcome()) {
            case TestOutcome.Ok(int expectedResult) ->
                    assertEquals(expectedResult, discoverablePhenotypeIds.size(),
                            "Incorrect evaluation for: " + testCase.description());
//            case TestOutcome.Error(Supplier<? extends RuntimeException> exceptionSupplier) ->
//                    assertThrows(exceptionSupplier.get().getClass(),
//                            () -> discoverablePhenotypeIds,
//                            "Incorrect error handling for: " + testCase.description());
        }
    }
}
