package org.monarchinitiative.maxodiff.core.analysis;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.StringWriter;
import java.util.List;

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

        List<RankedMaxoResult> results = createResults();

        generator.writeObject(results);

        String expected = """
                [ {
                  "maxoTerm" : {
                    "termId" : "MAXO:123",
                    "termLabel" : "MAXO Label 123"
                  },
                  "maxoScore" : 2.0,
                  "rankedOmimTermList" : [ {
                    "omimTerm" : {
                      "termId" : "OMIM:256000",
                      "termLabel" : "OMIM 256 Label"
                    },
                    "initialRank" : 5,
                    "averageRank" : 1.0
                  }, {
                    "omimTerm" : {
                      "termId" : "OMIM:128000",
                      "termLabel" : "OMIM 128 Label"
                    },
                    "initialRank" : 4,
                    "averageRank" : 2.0
                  } ],
                  "hpoTermIds" : [ {
                    "hpoTerm" : {
                      "termId" : "HPO:123000",
                      "termLabel" : "HPO 123 Label"
                    },
                    "count" : 50
                  }, {
                    "hpoTerm" : {
                      "termId" : "HPO:425000",
                      "termLabel" : "HPO 425 Label"
                    },
                    "count" : 85
                  } ],
                  "frequencies" : [ {
                    "omimId" : "OMIM:256000",
                    "hpoId" : "HPO:123000",
                    "frequency" : 0.5,
                    "mica" : 5.3
                  }, {
                    "omimId" : "OMIM:128000",
                    "hpoId" : "HPO:425000",
                    "frequency" : 0.75,
                    "mica" : 10.6
                  } ]
                } ]""";
        assertThat(writer.toString().replaceAll("\r", ""), equalTo(expected));
    }

    private static List<RankedMaxoResult> createResults() {
        return List.of(new RankedMaxoResult(new SimpleTerm("MAXO:123", "MAXO Label 123"),
                        2.,
                        List.of(new RankedOmimTerm(new SimpleTerm("OMIM:256000", "OMIM 256 Label"), 5, 1), new RankedOmimTerm(new SimpleTerm("OMIM:128000", "OMIM 128 Label"), 4,2)),
                        List.of(new CountedHpoTerm(new SimpleTerm("HPO:123000", "HPO 123 Label"), 50), new CountedHpoTerm(new SimpleTerm("HPO:425000", "HPO 425 Label"), 85)),
                        List.of(new HpoFrequency("OMIM:256000", "HPO:123000", 0.5f, 5.3f), new HpoFrequency("OMIM:128000", "HPO:425000", 0.75f, 10.6f))
                        )
        );
    }
}
