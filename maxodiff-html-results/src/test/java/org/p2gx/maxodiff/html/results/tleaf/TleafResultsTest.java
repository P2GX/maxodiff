package org.p2gx.maxodiff.html.results.tleaf;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.*;
import org.p2gx.maxodiff.core.model.TermPair;
import org.p2gx.maxodiff.core.phenomizer.IcMicaData;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renders the real templates from a small fixture and checks what the page says, not how it is laid out,
 * so that the markup can be restructured without breaking these tests.
 */
class TleafResultsTest {

    private static final SimpleTerm MARFAN = new SimpleTerm(TermId.of("OMIM:154700"), "Marfan syndrome");
    private static final SimpleTerm LDS4 = new SimpleTerm(TermId.of("OMIM:616914"), "Loeys-Dietz syndrome 4");
    private static final SimpleTerm LDS3 = new SimpleTerm(TermId.of("OMIM:609008"), "Loeys-Dietz syndrome 3");

    private static final SimpleTerm ARACHNODACTYLY = new SimpleTerm(TermId.of("HP:0001166"), "Arachnodactyly");
    private static final SimpleTerm PECTUS = new SimpleTerm(TermId.of("HP:0000767"), "Pectus excavatum");

    private static final SimpleTerm TALL_STATURE = new SimpleTerm(TermId.of("HP:0000098"), "Tall stature");
    private static final SimpleTerm SEIZURE = new SimpleTerm(TermId.of("HP:0001250"), "Seizure");

    private static Document page;

    @BeforeAll
    static void renderPage() {
        List<RankedMaxoResult> results = List.of(
                result("MAXO:0000101", "Chest MRI", 8.46),
                result("MAXO:0000102", "Echocardiography", 4.21),
                result("MAXO:0000103", "Never shown", 0.0));   // a score of 0 ends the list

        MdMetadata metadata = new MdMetadata("sample-B15", 3, 100,
                List.of(TALL_STATURE), List.of(SEIZURE), results);

        // maxMica is stored in IcMicaData; the frequency map needs no diseases because the fixture
        // provides a frequency record for every disease/term pair
        IcMicaData icData = new IcMicaData(Map.of(TermPair.symmetric(ARACHNODACTYLY.tid(), PECTUS.tid()), 9.0), null);
        HTMLFrequencyMap frequencyMap = new HTMLFrequencyMap(null, icData);

        page = Jsoup.parse(TleafResults.writeHTMLResults(metadata, results, frequencyMap));
    }

    private static RankedMaxoResult result(String maxoId, String maxoLabel, double score) {
        List<RankedOmimTerm> diseases = List.of(
                new RankedOmimTerm(MARFAN, 1, 3.0f),
                new RankedOmimTerm(LDS4, 2, 1.0f),
                new RankedOmimTerm(LDS3, 3, 2.0f));
        List<CountedHpoTerm> terms = List.of(
                new CountedHpoTerm(ARACHNODACTYLY, 60),
                new CountedHpoTerm(PECTUS, 25));
        List<HpoFrequency> frequencies = List.of(
                new HpoFrequency(MARFAN.tid(), ARACHNODACTYLY.tid(), 0.5f, 3.0f),
                new HpoFrequency(MARFAN.tid(), PECTUS.tid(), 0.6f, 4.0f),
                new HpoFrequency(LDS4.tid(), ARACHNODACTYLY.tid(), 0.4f, 2.0f),
                new HpoFrequency(LDS4.tid(), PECTUS.tid(), 0.3f, 5.0f),
                new HpoFrequency(LDS3.tid(), ARACHNODACTYLY.tid(), 0.2f, 1.0f),
                new HpoFrequency(LDS3.tid(), PECTUS.tid(), 0.1f, 6.0f));
        return new RankedMaxoResult(new SimpleTerm(TermId.of(maxoId), maxoLabel), score, diseases, terms, frequencies);
    }

    @Test
    void theSampleIdAndTheObservedAndExcludedTermsAreShown() {
        assertThat(page.selectFirst("h1").text()).isEqualTo("sample-B15");
        assertThat(page.text()).contains("Tall stature", "Seizure");
    }

    @Test
    void everyMaxoTermDiseaseAndHpoTermIsShown() {
        assertThat(page.text()).contains("Chest MRI", "Echocardiography");
        assertThat(page.text()).contains("Marfan syndrome", "Loeys-Dietz syndrome 4", "Loeys-Dietz syndrome 3");
        assertThat(page.text()).contains("Arachnodactyly", "Pectus excavatum");
    }

    @Test
    void theDeltaScoreIsShownWithOneDecimal() {
        assertThat(page.text()).contains("8.5", "4.2");
    }

    @Test
    void resultsFromTheFirstZeroScoreOnwardAreNotShown() {
        assertThat(page.text()).doesNotContain("Never shown");
    }

    @Test
    void noTemplateExpressionIsLeftUnresolved() {
        assertThat(page.text()).doesNotContain("${").doesNotContain("[[").doesNotContain("th:");
    }

    @Test
    void diseasesLinkToOmim() {
        assertThat(page.select("a[href^=https://www.omim.org/entry/]")).isNotEmpty();
        assertThat(page.select("a[href=https://www.omim.org/entry/154700]")).isNotEmpty();
    }

    @Test
    void aCountOfDiseasesIsShownAsAWholeNumber() {
        assertThat(page.text()).contains("N Diseases 3 ").doesNotContain("N Diseases 3.0");
    }

    @Test
    void numbersUseADotAsTheDecimalSeparator() {
        // the MICA values of the fixture are 3.0 and 4.0 for Marfan syndrome, shown with two decimals
        assertThat(page.text()).contains("3.00").contains("4.00").doesNotContain("3,00");
    }
}