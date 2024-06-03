package org.monarchinitiative.maxodiff.html.service;

import org.monarchinitiative.maxodiff.core.analysis.Frequencies;
import org.monarchinitiative.maxodiff.core.analysis.LiricalResultsFileRecord;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermMap;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermScore;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class DifferentialDiagnosisService {


    public DifferentialDiagnosisService() {}


    public List<MaxoTermScore> getMaxoTermRecords(MaxoTermMap maxoTermMap, List<LiricalResultsFileRecord> liricalOutputRecords,
                                                  Path phenopacketPath, int nDiseases, double weight) throws Exception {
        return maxoTermMap.getMaxoTermRecords(phenopacketPath, null, liricalOutputRecords, nDiseases, weight);
    }

    public List<Frequencies> getFrequencyRecords(MaxoTermMap maxoTermMap, MaxoTermScore maxoTermScore) throws Exception {
        return maxoTermMap.getFrequencyRecords(maxoTermScore);
    }


    public TermId getDiseaseId(MaxoTermMap maxoTermMap) {
        return maxoTermMap.getDiseaseId();
    }

}
