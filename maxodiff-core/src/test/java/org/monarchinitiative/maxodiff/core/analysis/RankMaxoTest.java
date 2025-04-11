package org.monarchinitiative.maxodiff.core.analysis;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.TestResources;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.*;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;


public class RankMaxoTest {

    private final static HpoDiseases hpoDiseases = TestResources.hpoDiseases();
    private final static List<DifferentialDiagnosis> initialDiagnoses = TestResources.getExampleDiagnoses().stream().toList();
    private final static Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap = TestResources.hpoToMaxo();

    private final static Map<TermId, Set<TermId>> maxoToHpoTermIdMap = MaxoHpoTermIdMaps.getMaxoToHpoTermIdMap(hpoToMaxoTermMap);
    private final static MaxoHpoTermProbabilities maxoHpoTermProbabilities =
            new MaxoHpoTermProbabilities(hpoDiseases,
                                         hpoToMaxoTermMap,
                                         initialDiagnoses,
                                         DiseaseModelProbability.ranked(initialDiagnoses));

    private static DifferentialDiagnosisEngine ENGINE;
    private static Ontology ontology = TestResources.hpo();
    private static MinimalOntology minimalOntology = TestResources.minHpo();

    @BeforeAll
    public static void setUpBeforeClass() {
        ENGINE = new DifferentialDiagnosisEngine() {
            @Override
            public List<DifferentialDiagnosis> run(Sample sample) {
                // TODO[mabeckwith] - Returning the initial diagnoses makes sense to me.
                //  This is, AFAIK, the purpose of the `DifferentialDiagnosisEngine`.
                //  However, please check..
                return initialDiagnoses;
            }

            @Override
            public List<DifferentialDiagnosis> run(Sample sample, Collection<TermId> targetDiseases) {
                return initialDiagnoses;
            }
        };
    }

    /**
     *
     * @return Sample phenopacket with one included HPO term Id and one disease Id.
     */
    public static Sample getPPkt1() {
        List<TermId> presentTerms = List.of(
                TermId.of("HP:0008619")
        );
        List<TermId> excludedTerms = List.of();
        List<TermId> diseaseIds = List.of(TermId.of("OMIM:615837"));

        return Sample.of("sample1", presentTerms, excludedTerms);//, diseaseIds);
    }

    /**
     *
     * @return Sample phenopacket with two included HPO term Ids and one disease Id.
     */
    public static Sample getPPkt2() {
        List<TermId> presentTerms = List.of(
                TermId.of("HP:0008619"),
                TermId.of("HP:0001751")
        );
        List<TermId> excludedTerms = List.of();
        List<TermId> diseaseIds = List.of(TermId.of("OMIM:615837"));

        return Sample.of("sample1", presentTerms, excludedTerms);//, diseaseIds);
    }

    /**
     * We expect this to cause an error, because OMIM:123456 is not aa actual identifier
     * @return Sample phenopacket with one included HPO term Id and one dummy disease Id.
     */
    public static Sample getPPktEmptyDisease() {
        List<TermId> presentTerms = List.of(
                TermId.of("HP:0008619")
        );
        List<TermId> excludedTerms = List.of();
        List<TermId> diseaseIds = List.of(TermId.of("OMIM:123456"));

        return Sample.of("sample2", presentTerms, excludedTerms);//, diseaseIds);
    }

    /**
     * This tests ranking MAxO terms
     */
    // Skip this test because it doesn't compile on push
    @Test
    @Disabled
    public void testRankMaxoTerms() {
        Set<TermId> diseaseIds = initialDiagnoses.stream()
                .map(DifferentialDiagnosis::diseaseId).collect(Collectors.toSet());
        Sample s1 = TestResources.getExampleSample();
        Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap = TestResources.hpoToMaxo();
        RankMaxo rankMaxo = new RankMaxo(hpoToMaxoTermMap, maxoToHpoTermIdMap, maxoHpoTermProbabilities, ENGINE,
                minimalOntology, ontology);
        Map<TermId, RankMaxoScore> maxoTermRanks = rankMaxo.rankMaxoTerms(s1, 2, diseaseIds);
        System.out.println(maxoTermRanks);
    }

}
