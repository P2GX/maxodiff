package org.monarchinitiative.maxodiff.html.service;

import org.monarchinitiative.maxodiff.core.analysis.LiricalResultsFileRecord;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermScoreMap;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class DifferentialDiagnosisService {


    public DifferentialDiagnosisService() {}


    public List<MaxoTermScoreMap.MaxoTermScore> getMaxoTermRecords(MaxoTermScoreMap maxoTermScoreMap, List<LiricalResultsFileRecord> liricalOutputRecords,
                                                                   Path phenopacketPath, double posttestFilter, double weight) throws Exception {
        return maxoTermScoreMap.getMaxoTermRecords(phenopacketPath, liricalOutputRecords, posttestFilter, weight);
    }

    public List<MaxoTermScoreMap.Frequencies> getFrequencyRecords(MaxoTermScoreMap maxoTermScoreMap, MaxoTermScoreMap.MaxoTermScore maxoTermScore) throws Exception {
        return maxoTermScoreMap.getFrequencyRecords(maxoTermScore);
    }


    public TermId getDiseaseId(MaxoTermScoreMap maxoTermScoreMap) {
        return maxoTermScoreMap.getDiseaseId();
    }

}
