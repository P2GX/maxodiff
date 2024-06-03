package org.monarchinitiative.maxodiff.core.model;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.maxodiff.core.analysis.DiseaseTermCount;
import org.monarchinitiative.maxodiff.core.TestResources;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DiseaseTermCountTest {

    private static final HpoDiseases hpoDiseases = TestResources.hpoDiseases();
    private static final List<HpoDisease> diseases = new ArrayList<>();
    private static DiseaseTermCount diseaseTermCount = null;

    @BeforeAll
    public static void setUp() {
        List<TermId> diseaseIds = List.of(TermId.of("OMIM:164745"), TermId.of("OMIM:216300"), TermId.of("OMIM:616684"));
        diseaseIds.forEach(id -> hpoDiseases.diseaseById(id).ifPresent(diseases::add));
        diseaseTermCount = DiseaseTermCount.of(diseases);
    }

    @Test
    public void diseaseTermCountExistsTest() { assertNotNull(diseaseTermCount); }

    @Test
    public void nDiseasesTest() { assertEquals(3, diseaseTermCount.nDiseases()); }

    @Test
    public void diseaseListTest() { assertEquals(diseases, diseaseTermCount.hpoDiseases()); }

    @Test
    public void nHpoTermsTest() { assertEquals(35, diseaseTermCount.nHpoTerms()); }

    @Test
    public void hpoTermCountTest() {
        Map<TermId, List<HpoFrequency>> termCounts = diseaseTermCount.hpoTermCounts();
        for (Map.Entry<TermId, List<HpoFrequency>> e : termCounts.entrySet()) {
            TermId id = e.getKey();
            List<HpoFrequency> frequencyList = e.getValue();
            int count = frequencyList.size();
            if (id.getId().equals("0000456")) {
                assertEquals(2, count);
            } else if (id.getId().equals("0003828")) {
                assertEquals(3, count);
            } else {
                assertEquals(1, count);
            }
        }
    }


}
