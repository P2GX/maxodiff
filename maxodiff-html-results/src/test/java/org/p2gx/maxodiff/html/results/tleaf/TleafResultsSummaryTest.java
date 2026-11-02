package org.p2gx.maxodiff.html.results.tleaf;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.*;
import org.p2gx.maxodiff.core.model.TermPair;
import org.p2gx.maxodiff.core.phenomizer.IcMicaData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TleafResultsSummaryTest {

    private static final SimpleTerm MARFAN = new SimpleTerm(TermId.of("OMIM:154700"), "Marfan syndrome");
    private static final SimpleTerm LDS4 = new SimpleTerm(TermId.of("OMIM:616914"), "Loeys-Dietz syndrome 4");
    private static final SimpleTerm ARACHNODACTYLY = new SimpleTerm(TermId.of("HP:0001166"), "Arachnodactyly");

    private static RankedMaxoResult result(int n, double score) {
        List<RankedOmimTerm> diseases = List.of(new RankedOmimTerm(MARFAN, 1, 3.0f), new RankedOmimTerm(LDS4, 2, 1.0f));
        List<CountedHpoTerm> terms = List.of(new CountedHpoTerm(ARACHNODACTYLY, 60));
        List<HpoFrequency> frequencies = List.of(
                new HpoFrequency(MARFAN.tid(), ARACHNODACTYLY.tid(), 0.5f, 3.0f),
                new HpoFrequency(LDS4.tid(), ARACHNODACTYLY.tid(), 0.4f, 2.0f));
        return new RankedMaxoResult(new SimpleTerm(TermId.of(String.format("MAXO:%07d", n)), "Test " + n),
                score, diseases, terms, frequencies);
    }

    private static Document render(List<RankedMaxoResult> results) {
        MdMetadata metadata = new MdMetadata("sample-B15", 2, 100, List.of(), List.of(), results);
        IcMicaData icData = new IcMicaData(Map.of(TermPair.symmetric(ARACHNODACTYLY.tid(), MARFAN.tid()), 9.0), null);
        return Jsoup.parse(TleafResults.writeHTMLResults(metadata, results, new HTMLFrequencyMap(null, icData)));
    }

    private static List<RankedMaxoResult> thirteenResults() {
        List<RankedMaxoResult> results = new ArrayList<>();
        for (int i = 1; i <= 13; i++) {
            results.add(result(i, 14.0 - i));   // scores 13.0 ... 1.0, best first
        }
        return results;
    }

    @Test
    void onlyTheBestTenAreShownInFullDetail() {
        Document page = render(thirteenResults());

        assertThat(page.select("table.countsTable")).hasSize(10);
        assertThat(page.select("table.tornado")).hasSize(10);
        assertThat(page.select("table.matrix thead th.m-col")).hasSize(2);    // the two diseases
        assertThat(page.select("table.matrix tbody tr")).hasSize(13);          // all 13 tests (the limit is 25)
        assertThat(page.select("h2").text()).contains("10) MAXO:0000010: Test 10")
                .doesNotContain("MAXO:0000011");
    }

    @Test
    void theOtherTermsAreListedWithTheirPositionAndScore() {
        Document page = render(thirteenResults());

        assertThat(page.select("table.summaryTable tbody tr")).hasSize(3);
        Element first = page.select("table.summaryTable tbody tr").first();
        assertThat(first.select("td").get(0).text()).isEqualTo("11");
        assertThat(first.text()).contains("Test 11", "MAXO:0000011", "3.0");
    }

    @Test
    void theNoteSaysHowManyAreShownInDetail() {
        assertThat(render(thirteenResults()).text())
                .contains("Full details are shown for the 10 highest-scoring tests out of 13");
    }

    @Test
    void withTenOrFewerResultsThereIsNoSummaryTable() {
        Document page = render(thirteenResults().subList(0, 10));
        assertThat(page.select("table.summaryTable")).isEmpty();
        assertThat(page.text()).doesNotContain("Full details are shown for the");
    }

    @Test
    void whenNoTestChangesTheRankingThePageSaysSoInsteadOfFailing() {
        Document page = render(List.of(result(1, 0.0)));
        assertThat(page.text()).contains("None of the tests is expected to change the ranking");
        assertThat(page.select("table.countsTable")).isEmpty();
    }

    @Test
    void theNumberOfDetailedTermsCanBeChosen() {
        List<RankedMaxoResult> results = thirteenResults();
        MdMetadata metadata = new MdMetadata("sample-B15", 2, 100, List.of(), List.of(), results);
        IcMicaData icData = new IcMicaData(Map.of(TermPair.symmetric(ARACHNODACTYLY.tid(), MARFAN.tid()), 9.0), null);

        Document page = Jsoup.parse(TleafResults.writeHTMLResults(metadata, results, new HTMLFrequencyMap(null, icData), 3));

        assertThat(page.select("table.countsTable")).hasSize(3);
        assertThat(page.select("table.summaryTable tbody tr")).hasSize(10);
    }

    private static Document render(MdMetadata metadata, List<RankedMaxoResult> results) {
        IcMicaData icData = new IcMicaData(Map.of(TermPair.symmetric(ARACHNODACTYLY.tid(), MARFAN.tid()), 9.0), null);
        return Jsoup.parse(TleafResults.writeHTMLResults(metadata, results, new HTMLFrequencyMap(null, icData)));
    }

    private static final SimpleTerm SEIZURE = new SimpleTerm(TermId.of("HP:0001250"), "Seizure");

    @Test
    void theHeaderListsObservedAndExcludedTermsAsLinks() {
        MdMetadata metadata = new MdMetadata("PMID_1_Patient_5", 20, 80,
                List.of(ARACHNODACTYLY), List.of(SEIZURE), thirteenResults());

        Document page = render(metadata, thirteenResults());

        assertThat(page.select("h1").text()).isEqualTo("PMID_1_Patient_5");
        assertThat(page.select(".patient-card a.term.observed").eachText()).containsExactly("Arachnodactyly");
        assertThat(page.select(".patient-card a.term.excluded").eachText()).containsExactly("Seizure");
        assertThat(page.select(".patient-card a.term.observed").attr("href"))
                .isEqualTo("https://hpo.jax.org/browse/term/HP:0001166");
    }

    @Test
    void theNumberOfDiseasesAndSimulationsAreInTheNoteAndNotInTheHeader() {
        MdMetadata metadata = new MdMetadata("p", 20, 80, List.of(ARACHNODACTYLY), List.of(), thirteenResults());

        Document page = render(metadata, thirteenResults());

        assertThat(page.select(".patient-card").text()).doesNotContain("Diseases", "Simulations");
        assertThat(page.select("p.run-note").text())
                .contains("The 20 most likely diseases were compared, with 80 simulations per test.");
    }

    @Test
    void emptyTermListsSayNone() {
        MdMetadata metadata = new MdMetadata("p", 20, 80, List.of(), List.of(), thirteenResults());

        assertThat(render(metadata, thirteenResults()).select(".patient-card .none")).hasSize(2);
    }

    @Test
    void theMatrixShowsTheRankChangeOfEachDiseaseForEachTest() {
        Element firstRow = render(thirteenResults()).select("table.matrix tbody tr").first();
        assertThat(firstRow.select("th.m-row .m-label").text()).isEqualTo("Test 1");
        assertThat(firstRow.select("td.m-cell").eachText()).containsExactly("+2.0", "−1.0");
    }
}
