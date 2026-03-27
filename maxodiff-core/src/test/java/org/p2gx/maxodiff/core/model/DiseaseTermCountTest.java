package org.p2gx.maxodiff.core.model;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.p2gx.maxodiff.core.analysis.DiseaseTermCount;
import org.p2gx.maxodiff.core.TestResources;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.HpoFrequency;

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
        List<TermId> diseaseIds = List.of(TermId.of("OMIM:619340"), TermId.of("OMIM:304050"),
                TermId.of("OMIM:613254"), TermId.of("OMIM:309400"));
        diseaseIds.forEach(id -> hpoDiseases.diseaseById(id).ifPresent(diseases::add));
        diseaseTermCount = DiseaseTermCount.of(diseases);
    }

    @Test
    public void diseaseTermCountExistsTest() { assertNotNull(diseaseTermCount); }

    @Test
    public void nDiseasesTest() { assertEquals(4, diseaseTermCount.nDiseases()); }

    @Test
    public void diseaseListTest() { assertEquals(diseases, diseaseTermCount.hpoDiseases()); }

    @Test
    public void nHpoTermsTest() { assertEquals(114, diseaseTermCount.nHpoTerms()); }

    @Test
    public void hpoTermCountTest() {
        Map<String, List<HpoFrequency>> termCounts = diseaseTermCount.hpoTermCounts();
        for (Map.Entry<String, List<HpoFrequency>> e : termCounts.entrySet()) {
            String id = e.getKey();
            List<HpoFrequency> frequencyList = e.getValue();
            int count = frequencyList.size();
            if (id.equals("HP:0012469") | id.equals("HP:0000252") | id.equals("HP:0002187")
                    | id.equals("HP:0001252") | id.equals("HP:0001249") | id.equals("HP:0000826")) {
                assertEquals(2, count);
            } else if (id.equals("HP:0001250")) {
                assertEquals(3, count);
            } else if (id.equals("HP:0011097")) {
                assertEquals(4, count);
            } else {
                assertEquals(1, count);
            }
        }
    }


}
