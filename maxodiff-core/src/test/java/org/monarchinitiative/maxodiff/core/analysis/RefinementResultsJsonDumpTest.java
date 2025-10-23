package org.monarchinitiative.maxodiff.core.analysis;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementResults;
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
                    "rankMaxoScore" : {
                      "maxoId" : "MAXO:123",
                      "initialOmimTermIds" : [ "OMIM:256000" ],
                      "maxoOmimTermIds" : [ "OMIM:128000" ],
                      "discoverableObservedHpoTermIds" : [ ],
                      "chosenHpoTermCtsMap" : { },
                      "maxoScore" : 2.0,
                      "maxoDiagnoses" : [ ],
                      "hpoTermIdRepCtsMap" : { },
                      "maxoDiseaseAvgRankChangeMap" : { },
                      "minRankChange" : 0,
                      "maxRankChange" : 0
                    },
                    "frequencies" : [ {
                      "hpoId" : "HP:123",
                      "frequencies" : [ 1.0, 4.6, 8.19 ]
                    } ]
                  } ]
                }""";
        assertThat(writer.toString().replaceAll("\r", ""), equalTo(expected));
    }

    private static RefinementResults createResults() {
        return RefinementResults.of(
                List.of(
                        MaxodiffResult.of(
                                new RankMaxoScore(
                                        TermId.of("MAXO:123"),
                                        Set.of(TermId.of("OMIM:256000")),
                                        Set.of(TermId.of("OMIM:128000")),
                                        Set.of(),
                                        Map.of(),
                                        2.,
                                        List.of(),
                                        Map.of(),
                                        Map.of(),
                                        0,
                                        0
                                ),
                                List.of(
                                        new Frequencies(TermId.of("HP:123"), List.of(1.f, 4.6f, 8.19f))
                                )
                        )
                ));
    }
}
