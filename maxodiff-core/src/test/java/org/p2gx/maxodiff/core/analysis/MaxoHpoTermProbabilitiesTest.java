package org.p2gx.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;
import org.p2gx.maxodiff.core.TestResources;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;
import org.p2gx.maxodiff.core.model.MaxoHpoTermProbabilities;
import org.p2gx.maxodiff.core.model.PhenopacketData;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MaxoHpoTermProbabilitiesTest {

    private final static HpoDiseases hpoDiseases = TestResources.hpoDiseases();
    private final static Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap = TestResources.hpoToMaxo();
    private final static List<DifferentialDiagnosis> initialDiagnoses = TestResources.getExampleDiagnoses().stream().toList(); //top K diagnoses only
    private final static PhenopacketData samplePhenopacket = TestResources.getExampleSample();

    private final static MaxoHpoTermProbabilities MAXO_HPO_TERM_PROBABILITIES = new MaxoHpoTermProbabilities(hpoDiseases,
            hpoToMaxoTermMap, initialDiagnoses);

    @Test
    public void testUnionDiscoverablePhenotypes() {
        Set<TermId> union = MAXO_HPO_TERM_PROBABILITIES.getUnionOfDiscoverablePhenotypes(samplePhenopacket);
        assertEquals(332, union.size(), 1e-3);
    }

    @Test
    public void testMaxoTermBenefitIds() {
        Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap = TestResources.hpoToMaxo();
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = MaxoHpoTermIdMaps.getMaxoToHpoTermIdMap(hpoToMaxoTermMap);
        TermId maxoId = TermId.of("MAXO:0035006"); //Foot radiography
        Set<TermId> maxoBenefitIds = MAXO_HPO_TERM_PROBABILITIES.getDiscoverableByMaxoHpoTerms(samplePhenopacket, maxoId, maxoToHpoTermIdMap);
        assertEquals(9, maxoBenefitIds.size(), 1e-3);
    }

}
