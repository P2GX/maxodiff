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
                    "maxoTermScore" : {
                      "maxoId" : "MAXO:123",
                      "nOmimTerms" : 1,
                      "omimTermIds" : [ "OMIM:256000" ],
                      "maxoOmimTermIds" : [ "OMIM:128000" ],
                      "nHpoTerms" : 0,
                      "hpoTermIds" : [ ],
                      "initialScore" : 1.0,
                      "score" : 3.0,
                      "scoreDiff" : 2.0,
                      "changedDiseaseId" : "OMIM:640000",
                      "maxoDiagnoses" : [ ],
                      "initialDiagnosesMaxoOrdered" : [ ],
                      "originalCDF" : [ 0.0 ],
                      "maxoTermCDF" : [ 0.0 ]
                    },
                    "rankMaxoScore" : {
                      "maxoId" : "MAXO:123",
                      "initialOmimTermIds" : [ "OMIM:256000" ],
                      "maxoOmimTermIds" : [ "OMIM:128000" ],
                      "discoverableObservedHpoTermIds" : [ ],
                      "maxoScore" : 2.0,
                      "maxoDiagnoses" : [ ],
                      "hpoTermIdRepCtsMap" : { },
                      "remainingHpoTermIdRepCtsMap" : { },
                      "maxoDiseaseAvgRankChangeMap" : { },
                      "minRankChange" : 0,
                      "maxRankChange" : 0
                    },
                    "frequencies" : [ {
                      "hpoId" : "HP:123",
                      "frequencies" : [ 1.0, 4.6, 8.19 ]
                    } ],
                    "maxoFrequencies" : [ {
                      "hpoId" : "HP:246",
                      "frequencies" : [ 0.5, 2.3, 4.15 ]
                    } ]
                  } ]
                }""";
        assertThat(writer.toString().replaceAll("\r", ""), equalTo(expected));
    }

    private static RefinementResults createResults() {
        return RefinementResults.of(
                List.of(
                        MaxodiffResult.of(
                                new MaxoTermScore(
                                        "MAXO:123",
                                        1, Set.of(TermId.of("OMIM:256000")), Set.of(TermId.of("OMIM:128000")),
                                        0, Set.of(),
                                        1.,
                                        3.,
                                        2.,
                                        TermId.of("OMIM:640000"),
                                        List.of(), List.of(), new double[1], new double[1]
                                ),
                                new RankMaxoScore(
                                        TermId.of("MAXO:123"),
                                        Set.of(TermId.of("OMIM:256000")),
                                        Set.of(TermId.of("OMIM:128000")),
                                        Set.of(),
                                        2.,
                                        List.of(),
                                        Map.of(),
                                        Map.of(),
                                        Map.of(),
                                        0,
                                        0
                                ),
                                List.of(
                                        new Frequencies(TermId.of("HP:123"), List.of(1.f, 4.6f, 8.19f))
                                ),
                                List.of(
                                        new Frequencies(TermId.of("HP:246"), List.of(0.5f, 2.3f, 4.15f))
                                )
                        )
                ));
    }
}
