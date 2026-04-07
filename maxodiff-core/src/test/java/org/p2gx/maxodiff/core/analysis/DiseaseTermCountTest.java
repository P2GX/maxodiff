package org.p2gx.maxodiff.core.analysis;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.p2gx.maxodiff.core.TestResources.hpoDiseases;

public class DiseaseTermCountTest {

    private final static double TOLERANCE = 0.00000001;
    /** The following three entries have frequencies */
    private final static Set<TermId> targetDiseaseIds = Set.of(TermId.of("OMIM:616559"), // Noonan syndrome 9
            TermId.of("OMIM:615355"), // Noonan syndrome 8
            TermId.of("OMIM:613224") // Noonan syndrome 6
            );

    static private final List<HpoDisease> hpoDiseaseList = hpoDiseases().
            stream().
            filter(hpo_dis ->targetDiseaseIds.contains(hpo_dis.id())).
            toList();

    static private final DiseaseTermCount dtcount = DiseaseTermCount.of(hpoDiseaseList);

    @Test
    public void testConstructor() {
        assertNotNull(dtcount);
    }

    @Test
    public void test_got_three_diseases() {
        assertEquals(3, dtcount.nDiseases());
    }

    /**
     * HP:0004523 Long eyebrows - found in Noonan syndrome 6   OMIM:613224  but not the other two diseases
     * The frequency is not provided in the HPOAs, and should be inferred to be 1.0
     */
    @Test
    public void test_long_eyebrows() {
        List<HpoFrequency> hpoFrequencyList = dtcount.hpoTermCounts();
        List<HpoFrequency> hpofreqList1 = hpoFrequencyList.stream().filter(f -> f.hpoId().equals("HP:0004523")).toList();
        assertEquals(1, hpofreqList1.size());
        HpoFrequency hpofreq = hpofreqList1.getFirst();
        assertEquals("OMIM:613224", hpofreq.omimId());
        assertEquals(1.0f, hpofreq.frequency(), TOLERANCE);
    }

    /**
     * Epicanthus HP:0000286
     * Noonan syndrome 8    OMIM:615355 - frequency 4/6
     * Noonan syndrome 9   OMIM:616559 -- not annotated
     * Noonan syndrome 6   OMIM:613224 -- annotated but no frequency supplied, assumed to be 1.0
     */
    @Test
    public void test_epicanthus() {
        List<HpoFrequency> hpoFrequencyList = dtcount.hpoTermCounts();
        List<HpoFrequency> hpofreqList1 = hpoFrequencyList.stream().filter(f -> f.hpoId().equals("HP:0000286")).toList();
        assertEquals(2, hpofreqList1.size());
        Optional<HpoFrequency> opt1 = hpofreqList1.stream().filter(hpf -> hpf.omimId().equals("OMIM:615355")).findFirst();
        assertTrue(opt1.isPresent());
        HpoFrequency noonan8 = opt1.get();
        // omimId=OMIM:613224, hpoId=HP:0004523, count=1, frequency=1.0]
        assertEquals("OMIM:615355", noonan8.omimId());
        assertEquals(4.0f/6.0f, noonan8.frequency(), TOLERANCE);
        Optional<HpoFrequency> opt2 = hpofreqList1.stream().filter(hpf -> hpf.omimId().equals("OMIM:613224")).findFirst();
        assertTrue(opt2.isPresent());
        HpoFrequency noonan6 = opt2.get();
        assertEquals("OMIM:613224", noonan6.omimId());
        assertEquals(1.0f, noonan6.frequency(), TOLERANCE);
    }

}
