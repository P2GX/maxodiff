package org.monarchinitiative.maxodiff.html.service;

import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.maxodiff.core.analysis.LiricalAnalysis;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermMap;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class MaxoTermService {


    public MaxoTermService() {}

    public AnalysisResults runLiricalCalculation(MaxoTermMap maxoTermMap, LiricalAnalysis liricalAnalysis, Path phenopacketPath) throws Exception {
        return maxoTermMap.runLiricalCalculation(liricalAnalysis, phenopacketPath);
    }

    public List<MaxoTermMap.MaxoTermScore> getMaxoTermRecords(MaxoTermMap maxoTermMap, AnalysisResults results,
                                                              Path phenopacketPath, double posttestFilter, double weight) throws Exception {
        return maxoTermMap.getMaxoTermRecords(phenopacketPath, results, null, posttestFilter, weight);
    }

    public List<MaxoTermMap.Frequencies> getFrequencyRecords(MaxoTermMap maxoTermMap, MaxoTermMap.MaxoTermScore maxoTermScore) throws Exception {
        return maxoTermMap.getFrequencyRecords(maxoTermScore);
    }


    public TermId getDiseaseId(MaxoTermMap maxoTermMap) {
        return maxoTermMap.getDiseaseId();
    }

}
