package org.monarchinitiative.maxodiff.phenomizer;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class IcMicaDictLoaderTest {

    private static final URL TOY_DICT = Objects.requireNonNull(IcMicaDictLoaderTest.class.getResource("tps.50lines.csv"));

    @Test
    public void loadIcMicaDict() throws IOException {
        IcMicaData data;
        try (InputStreamReader reader = new InputStreamReader(TOY_DICT.openStream())) {
            data = IcMicaDictLoader.loadIcMicaDict(reader);
        }

        Map<TermPair, Double> icMicaDict = data.icMicaDict();

        assertThat(icMicaDict, is(notNullValue()));
        assertThat(icMicaDict.size(), is(equalTo(47)));
        assertThat(icMicaDict.get(TermPair.symmetric(TermId.of("HP:0006055"), TermId.of("HP:0001230"))), is(closeTo(1.907927473, 5E-10)));

        assertThat(data.metadata(), is(notNullValue()));
        assertThat(data.metadata().hpoVersion(), is(equalTo("2025-03-03")));
        assertThat(data.metadata().hpoaVersion(), is(equalTo("2025-03-04")));
        assertThat(data.metadata().created(), is(equalTo(LocalDate.of(2025, 3, 5))));
    }
}