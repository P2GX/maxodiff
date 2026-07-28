package org.p2gx.maxodiff.cli.benchmarking;


import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;


public record BenchmarkResult(
        String phenopacketId,
        int nDisease,
        int nRepetitions,
        TermId topMaxoTermId,
        double maxoFinalScore,
        BenchmarkProcedure procedure,
        int topMaxoRandomIdx,
        double maxoFinalScoreRandom,
        int nMaxoTerms,
        int nMaxoTermsRandom,
        List<Integer> nDiscoverablePhenotypes,
        double maxoIcSum,
        double maxoIcSumRandom,
        int randomDiseaseIdx
        ) {


    public String maxoId() {
        return topMaxoTermId.getValue();
    }



    public static List<String> header() {
        return List.of("PhenopacketId",
                "nDiseases",
                "nRepetitions",
                "maxoTerm" ,
                "maxoFinalScore",
                "procedure",
                "maxoTermRandomIdx",
                "maxoFinalScoreRandom",
                "nMaxoTerms",
                "nMaxoTermsRandom",
                "nDiscoverablePhenotypes",
                "maxoIcSum",
                "maxoIcSumRandom",
                "randomDiseaseIndex"
               );
    }

    public static String getHeaderLine() {
        return String.join("\t", header());
    }

    public String getRow() {
        List<String> row = List.of(phenopacketId(), String.valueOf(nDisease()), String.valueOf(nRepetitions()), maxoId(), String.valueOf(maxoFinalScore()), procedure().name(), String.valueOf(topMaxoRandomIdx()), String.valueOf(maxoFinalScoreRandom()), String.valueOf(nMaxoTerms()), String.valueOf(nMaxoTermsRandom()), String.valueOf(nDiscoverablePhenotypes()), String.valueOf(maxoIcSum()), String.valueOf(maxoIcSumRandom()), String.valueOf(randomDiseaseIdx()+1));
        return String.join("\t", row);
    }

}
