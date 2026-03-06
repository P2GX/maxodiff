package org.monarchinitiative.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.maxodiff.core.SimpleTermOld;
import org.monarchinitiative.maxodiff.core.TestResources;
import org.monarchinitiative.maxodiff.core.model.*;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MaxoHpoTermProbabilitiesTest {

    private final static HpoDiseases hpoDiseases = TestResources.hpoDiseases();
    private final static Map<SimpleTermOld, Set<SimpleTermOld>> hpoToMaxoTermMap = TestResources.hpoToMaxo();
    private final static List<DifferentialDiagnosis> initialDiagnoses = TestResources.getExampleDiagnoses().stream().toList(); //top K diagnoses only
    private final static DiseaseModelProbability diseaseModelProbability = DiseaseModelProbability.ranked(initialDiagnoses);
    private final static PpktSample samplePhenopacket = TestResources.getExampleSample();

    private final static MaxoHpoTermProbabilities MAXO_HPO_TERM_PROBABILITIES = new MaxoHpoTermProbabilities(hpoDiseases,
            hpoToMaxoTermMap, initialDiagnoses, diseaseModelProbability);

    @Test
    public void testUnionDiscoverablePhenotypes() {
        Set<TermId> union = MAXO_HPO_TERM_PROBABILITIES.getUnionOfDiscoverablePhenotypes(samplePhenopacket);
        assertEquals(332, union.size(), 1e-3);
    }

    @Test
    public void testMaxoTermBenefitIds() {
        Map<SimpleTermOld, Set<SimpleTermOld>> hpoToMaxoTermMap = TestResources.hpoToMaxo();
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = MaxoHpoTermIdMaps.getMaxoToHpoTermIdMap(hpoToMaxoTermMap);
        TermId maxoId = TermId.of("MAXO:0035006"); //Foot radiography
        Set<TermId> maxoBenefitIds = MAXO_HPO_TERM_PROBABILITIES.getDiscoverableByMaxoHpoTerms(samplePhenopacket, maxoId, maxoToHpoTermIdMap);
        assertEquals(9, maxoBenefitIds.size(), 1e-3);
    }

    @Test
    public void testProbabilityMaxoTermRevealHpoTerm() {
        TermId hpoId = TermId.of("HP:0008138"); //Equinus calcaneus
        double probability = MAXO_HPO_TERM_PROBABILITIES.calculateProbabilityOfMaxoTermRevealingPresenceOfHpoTerm(hpoId);
        assertEquals(0.019, probability, 1e-3);
    }
}
