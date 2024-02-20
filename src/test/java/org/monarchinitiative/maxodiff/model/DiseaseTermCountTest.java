package org.monarchinitiative.maxodiff.model;


import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.maxodiff.DiseaseTermCountImpl;
import org.monarchinitiative.maxodiff.TestResources;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DiseaseTermCountTest {

    private static final HpoDiseases hpoDiseases = TestResources.hpoDiseases();
    private static final List<HpoDisease> diseases = new ArrayList<>();
    private static DiseaseTermCountImpl diseaseTermCountImpl = null;

    @BeforeAll
    public static void setUp() {
        List<TermId> diseaseIds = List.of(TermId.of("OMIM:164745"), TermId.of("OMIM:216300"), TermId.of("OMIM:616684"));
        diseaseIds.forEach(id -> hpoDiseases.diseaseById(id).ifPresent(diseases::add));
        diseaseTermCountImpl = new DiseaseTermCountImpl(diseases);
    }

    @Test
    public void diseaseTermCountExistsTest() { assertNotNull(diseaseTermCountImpl); }

    @Test
    public void nDiseasesTest() { assertEquals(3, diseaseTermCountImpl.nDiseases()); }

    @Test
    public void diseaseListTest() { assertEquals(diseases, diseaseTermCountImpl.hpoDiseases()); }

    @Test
    public void nHpoTermsTest() { assertEquals(35, diseaseTermCountImpl.nHpoTerms()); }

    @Test
    public void hpoTermCountTest() {
        Map<TermId, List<Object>> termCounts = diseaseTermCountImpl.hpoTermCounts();
        for (Map.Entry<TermId, List<Object>> e : termCounts.entrySet()) {
            TermId id = e.getKey();
            int count = (int) e.getValue().get(0);
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
