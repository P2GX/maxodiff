package org.monarchinitiative.maxodiff.cli.benchmarking;


import org.monarchinitiative.phenol.ontology.data.Term;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;


public record BenchmarkResult(
        String phenopacketId,
        int nDisease,
        int nRepetitions,
        TermId topMaxoTermId,
        double maxoFinalScore,
        BenchmarkProcedure procedure,
        int nMaxoTerms,
        int randomDiseaseIdx
        ) {


    public String maxoId() {
        return topMaxoTermId.getValue();
    }



    public static List<String> header() {
        return List.of("PhenopacketId",
                "nDisease",
                "nRepetitions",
                "maxoTerm" ,
                "maxoFinalScore",
                "procedure",
                "nMaxoTerms",
                "random disease index"
               );
    }

    public static String getHeaderLine() {
        return String.join("\n", header());
    }

    public String getRow() {
        List<String> row = List.of(phenopacketId(), String.valueOf(nDisease()), String.valueOf(nRepetitions()), String.valueOf(maxoFinalScore()), procedure.name(), String.valueOf(nMaxoTerms), String.valueOf(randomDiseaseIdx()));
        return String.join("\n", row);
    }

}
