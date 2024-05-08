package org.monarchinitiative.maxodiff.html.service;

import org.monarchinitiative.maxodiff.core.analysis.LiricalResultsFileRecord;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermMap;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class DifferentialDiagnosisService {


    public DifferentialDiagnosisService() {}


    public List<MaxoTermMap.MaxoTerm> getMaxoTermRecords(MaxoTermMap maxoTermMap, List<LiricalResultsFileRecord> liricalOutputRecords,
                                                         Path phenopacketPath, double posttestFilter, double weight) throws Exception {
        return maxoTermMap.getMaxoTermRecords(phenopacketPath, null, liricalOutputRecords, posttestFilter, weight);
    }

    public List<MaxoTermMap.Frequencies> getFrequencyRecords(MaxoTermMap maxoTermMap, MaxoTermMap.MaxoTerm maxoTerm) throws Exception {
        return maxoTermMap.getFrequencyRecords(maxoTerm);
    }


    public TermId getDiseaseId(MaxoTermMap maxoTermMap) {
        return maxoTermMap.getDiseaseId();
    }

}
