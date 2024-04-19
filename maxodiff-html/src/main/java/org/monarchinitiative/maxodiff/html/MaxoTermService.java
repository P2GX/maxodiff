package org.monarchinitiative.maxodiff.html;

import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.maxodiff.core.analysis.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.analysis.DiseaseTermCountImpl;
import org.monarchinitiative.maxodiff.core.analysis.LiricalAnalysis;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermMap;
import org.monarchinitiative.maxodiff.core.io.MaxodiffDataException;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class MaxoTermService {

//    @Autowired
//    MaxoTermMap maxoTermMap = new MaxoTermMap(Path.of("/Users/beckwm/Desktop/maxodiff_tests/data/maxodiff"));

    LiricalAnalysis liricalAnalysis = new LiricalAnalysis("hg19", TranscriptDatabase.REFSEQ, .8f,
            0.1, true, false, Path.of("/Users/beckwm/Desktop/maxodiff_tests/data/lirical"),
            //Path.of("/Users/beckwm/Exomiser/2109_hg19/2109_hg19/2109_hg19_variants.mv.db"),
            //Path.of("/Users/beckwm/vcf/project.NIST.hc.snps.indels.NIST7035.vcf"));
            null, null);

    MaxoTermMap maxoTermMap = new MaxoTermMap(Path.of("/Users/beckwm/Desktop/maxodiff_tests/data/maxodiff"));

    DifferentialDiagnosis differentialDiagnosis = new DifferentialDiagnosis();

    public MaxoTermService() throws MaxodiffDataException {
    }

//    public MaxoTermService(Path maxoDataPath) throws MaxodiffDataException {
//        maxoTermMap = new MaxoTermMap(maxoDataPath);
//    }

    public List<MaxoTermMap.MaxoTerm> getMaxoTermRecords(Path phenopacketPath, double posttestFilter, double weight) throws Exception {
        return maxoTermMap.getMaxoTermRecords(liricalAnalysis, differentialDiagnosis, phenopacketPath, posttestFilter, weight);
    }

    public List<MaxoTermMap.Frequencies> getFrequencyRecords(MaxoTermMap.MaxoTerm maxoTerm) throws Exception {
        return maxoTermMap.getFrequencyRecords(maxoTerm);
    }


    public TermId getDiseaseId() {
        return maxoTermMap.getDiseaseId();
    }

}
