package org.monarchinitiative.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.TestResources;
import org.monarchinitiative.maxodiff.core.model.*;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ValidationModelTest {

    private final static HpoDiseases hpoDiseases = TestResources.hpoDiseases();
    private final static List<DifferentialDiagnosis> initialDiagnoses = TestResources.getExampleDiagnoses().stream().toList();
    private final static Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap = TestResources.hpoToMaxo();

    private final static Map<TermId, Set<TermId>> maxoToHpoTermIdMap = MaxoHpoTermIdMaps.getMaxoToHpoTermIdMap(hpoToMaxoTermMap);
    private final static MaxoHpoTermProbabilities maxoHpoTermProbabilities =
            new MaxoHpoTermProbabilities(hpoDiseases,
                                         hpoToMaxoTermMap,
                                         initialDiagnoses,
                                         DiseaseModelProbability.ranked(initialDiagnoses));

    private final static List<DifferentialDiagnosis> maxoDiagnoses = TestResources.getExampleMaxoDiagnoses().stream().toList();


    /**
     * This tests validating the maxodiff analysis using the sum of differences in disease ranks.
     */
    @Test
    public void testRankDiffValidationModel() {
        double validationScore = ValidationModel.rankDiff(initialDiagnoses, maxoDiagnoses).validationScore();
        assertEquals(13.0, validationScore, 1e-3);
    }

    /**
     * This tests validating the maxodiff analysis using the weighted sum of differences in disease ranks.
     */
    @Test
    public void testWeightedRankDiffValidationModel() {
        double validationScore = ValidationModel.weightedRankDiff(initialDiagnoses, maxoDiagnoses).validationScore();
        assertEquals(1.005, validationScore, 1e-3);
    }

    /**
     * This tests validating the maxodiff analysis using the sum of differences in disease scores.
     */
    @Test
    public void testScoreDiffValidationModel() {
        double validationScore = ValidationModel.scoreDiff(initialDiagnoses, maxoDiagnoses).validationScore();
        assertEquals(8.487, validationScore, 1e-3);
    }

}
