package org.monarchinitiative.maxodiff.core.analysis;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class RefinementResultsJsonDumpTest {

    private static ObjectMapper OBJECT_MAPPER;

    @BeforeAll
    public static void beforeAll() {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        OBJECT_MAPPER.registerModule(new Jdk8Module());
    }

    @Test
    public void dumpToJsonWorks() throws Exception {
        StringWriter writer = new StringWriter();
        JsonGenerator generator = OBJECT_MAPPER.createGenerator(writer);

        RefinementResults results = createResults();

        generator.writeObject(results);

        String expected = """
                {
                  "maxodiffResults" : [ {
                    "maxoTermScore" : {
                      "maxoId" : "MAXO:123",
                      "maxoLabel" : "Some label",
                      "nOmimTerms" : 1,
                      "omimTermIds" : [ "OMIM:256000" ],
                      "nHpoTerms" : 0,
                      "hpoTermIds" : [ ],
                      "probabilityMap" : { },
                      "initialScore" : 1.0,
                      "score" : 3.0,
                      "scoreDiff" : 2.0
                    },
                    "frequencies" : [ {
                      "hpoId" : "HP:123",
                      "hpoLabel" : "Bla",
                      "frequencies" : [ 1.0, 4.6, 8.19 ]
                    } ]
                  } ]
                }""";
        assertThat(writer.toString(), equalTo(expected));
    }

    private static RefinementResults createResults() {
        return RefinementResults.of(
                List.of(
                        MaxodiffResult.of(
                                new MaxoTermScore(
                                        "MAXO:123",
                                        "Some label",
                                        1, Set.of(TermId.of("OMIM:256000")),
                                        0, Set.of(),
                                        Map.of(),
                                        1.,
                                        3.,
                                        2.
                                ),
                                List.of(
                                        new Frequencies(TermId.of("HP:123"), "Bla", List.of(1.f, 4.6f, 8.19f))
                                )
                        )
                ));
    }
}
