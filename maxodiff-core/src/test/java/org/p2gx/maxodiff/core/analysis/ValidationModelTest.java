package org.p2gx.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.TestResources;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ValidationModelTest {

    private final static List<DifferentialDiagnosis> initialDiagnoses = TestResources.getExampleDiagnoses().stream().toList();
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


}
