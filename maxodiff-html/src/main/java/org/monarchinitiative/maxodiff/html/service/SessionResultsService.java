package org.monarchinitiative.maxodiff.html.service;

import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.maxodiff.core.analysis.Frequencies;
import org.monarchinitiative.maxodiff.core.analysis.LiricalAnalysis;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermMap;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermScore;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class SessionResultsService {


    public SessionResultsService() {}

    public List<MaxoTermScore> getMaxoTermRecords(MaxoTermMap maxoTermMap, AnalysisResults results,
                                                  Path phenopacketPath, int nDiseases, double weight) throws Exception {
        return maxoTermMap.getMaxoTermRecords(phenopacketPath, results, null, nDiseases, weight);
    }

    public List<Frequencies> getFrequencyRecords(MaxoTermMap maxoTermMap, MaxoTermScore maxoTermScore) throws Exception {
        return maxoTermMap.getFrequencyRecords(maxoTermScore);
    }


    public TermId getDiseaseId(MaxoTermMap maxoTermMap) {
        return maxoTermMap.getDiseaseId();
    }

}
