package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.maxodiff.core.analysis.LiricalResultsFileRecord;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermMap;
import org.monarchinitiative.maxodiff.core.io.LiricalResultsFileParser;
import org.monarchinitiative.maxodiff.html.analysis.CumulativeDistributionChartData;
import org.monarchinitiative.maxodiff.html.config.MaxodiffProperties;
import org.monarchinitiative.maxodiff.html.service.DifferentialDiagnosisService;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Path;
import java.util.*;

@Controller("/")
public class DifferentialDiagnosisController {

    @Autowired
    private MaxodiffProperties properties;

    @Autowired
    DifferentialDiagnosisService differentialDiagnosisService;

    List<LiricalResultsFileRecord> liricalResultsFileRecords;

    @RequestMapping("/")
    public String showResults(@RequestParam(value = "maxodiffDir", required = false) Path maxodiffDir,
                              @RequestParam(value = "phenopacketPath", required = false) Path phenopacketPath,
                              @RequestParam(value = "liricalResultsPath", required = false) Path liricalResultsPath,
                             @RequestParam(value = "nDiseases", required = false) Integer nDiseases,
                             @RequestParam(value = "weight", required = false) Double weight,
                             @RequestParam(value = "nMaxoResults", required = false) Integer nMaxoResults,
                             Model model) throws Exception {

        if (maxodiffDir == null) {
            maxodiffDir = properties.createDataDirectory("maxodiff");
            properties.addToPropertiesFile("maxodiff-data-directory", maxodiffDir.toString());
        }
        model.addAttribute("maxodiffDir", maxodiffDir);
        MaxoTermMap maxoTermMap = new MaxoTermMap(maxodiffDir);
        model.addAttribute("phenopacketPath", phenopacketPath);
        model.addAttribute("liricalResultsPath", liricalResultsPath);
        int nLiricalResults = 10;
        model.addAttribute("nLiricalResults", nLiricalResults);
        if (liricalResultsPath != null) {
            liricalResultsFileRecords = LiricalResultsFileParser.read(liricalResultsPath);
            model.addAttribute("totalNDiseases", liricalResultsFileRecords.size());
            model.addAttribute("liricalOutputRecords", liricalResultsFileRecords.subList(0, nLiricalResults));
        }
        model.addAttribute("nDiseases", nDiseases);
        model.addAttribute("weight", weight);
        model.addAttribute("nMaxoResults", nMaxoResults);
        if (phenopacketPath != null & liricalResultsFileRecords != null &
                nDiseases != null & weight != null & nMaxoResults != null) {
            List<MaxoTermMap.MaxoTermScore> maxoTermScoreRecords = differentialDiagnosisService.getMaxoTermRecords(maxoTermMap, liricalResultsFileRecords,
                    phenopacketPath, nDiseases, weight);
            TermId diseaseId = differentialDiagnosisService.getDiseaseId(maxoTermMap);
            String phenopacketName = phenopacketPath.toFile().getName();
            model.addAttribute("phenopacket", phenopacketName);
            model.addAttribute("diseaseId", diseaseId);
            model.addAttribute("maxoRecords", maxoTermScoreRecords);

//            double posttestFilterLB = nDiseases/2;
//            double posttestFilterUB = nDiseases*2 < 1 ? nDiseases*2 : 1;
//            List<MaxoTermMap.MaxoTermScore> maxoTermScoreRecordsPosttestFilterLB =
//                    differentialDiagnosisService.getMaxoTermRecords(maxoTermMap, liricalResultsFileRecords,
//                    phenopacketPath, posttestFilterLB, weight);
//            List<MaxoTermMap.MaxoTermScore> maxoTermScoreRecordsPosttestFilterUB =
//                    differentialDiagnosisService.getMaxoTermRecords(maxoTermMap, liricalResultsFileRecords,
//                    phenopacketPath, posttestFilterUB, weight);
//            List<List<MaxoTermMap.MaxoTermScore>> allPosttestFilterScoreRecords =
//                    Arrays.asList(maxoTermScoreRecordsPosttestFilterLB, maxoTermScoreRecords, maxoTermScoreRecordsPosttestFilterUB);
//
//            double weightLB = weight/2;
//            double weightUB = weight*2 < 1 ? weight*2 : 1;
//            List<MaxoTermMap.MaxoTermScore> maxoTermScoreRecordsWeightLB =
//                    differentialDiagnosisService.getMaxoTermRecords(maxoTermMap, liricalResultsFileRecords,
//                    phenopacketPath, nDiseases, weightLB);
//            List<MaxoTermMap.MaxoTermScore> maxoTermScoreRecordsWeightUB =
//                    differentialDiagnosisService.getMaxoTermRecords(maxoTermMap, liricalResultsFileRecords,
//                    phenopacketPath, nDiseases, weightUB);
//            List<List<MaxoTermMap.MaxoTermScore>> allWeightScoreRecords =
//                    Arrays.asList(maxoTermScoreRecordsWeightLB, maxoTermScoreRecords, maxoTermScoreRecordsWeightUB);
//
//            String posttestKey = "nDiseases";
//            String weightKey = "weight";
//            model.addAttribute("posttestFilterKey", posttestKey);
//            model.addAttribute("weightKey", weightKey);
//            Map<String, List<List<CumulativeDistributionChartData.CumulativeDistributionRecord>>> cumulativeDistributionRecordMap = new HashMap<>();
//            cumulativeDistributionRecordMap.put(posttestKey, CumulativeDistributionChartData.makeDistRecordList(allPosttestFilterScoreRecords));
//            cumulativeDistributionRecordMap.put(weightKey, CumulativeDistributionChartData.makeDistRecordList(allWeightScoreRecords));
//
//            Map<String, List<String>> distributionDatasetLabels = new HashMap<>();
//            distributionDatasetLabels.put(posttestKey,
//                    Arrays.asList("Posttest Probability Threshold = " + posttestFilterLB,
//                            "Posttest Probability Threshold = " + nDiseases,
//                            "Posttest Probability Threshold = " + posttestFilterUB));
//            distributionDatasetLabels.put(weightKey, Arrays.asList("Weight = " + weightLB, "Weight = " + weight, "Weight = " + weightUB));
//
//            model.addAttribute("cumulativeDistributionRecords", cumulativeDistributionRecordMap);
//            model.addAttribute("distributionDatasetLabels", distributionDatasetLabels);

            Set<TermId> omimIds = maxoTermScoreRecords.get(0).omimTermIds();
            Map<TermId, String> omimTermMap = new LinkedHashMap<>();
            liricalResultsFileRecords.forEach(outputFileRecord -> {
                TermId omimId = outputFileRecord.omimId();
                String omimLabel = outputFileRecord.omimLabel();
                if (omimIds.contains(omimId)) {
                    omimTermMap.put(omimId, omimLabel);
                }
                if (omimId.equals(diseaseId)) {
                    model.addAttribute("diseaseLabel", omimLabel);
                }
            });
            model.addAttribute("omimTerms", omimTermMap);

            Map<MaxoTermMap.MaxoTermScore, List<MaxoTermMap.Frequencies>> maxoTables = new LinkedHashMap<>();
            int nDisplayed = maxoTermScoreRecords.size() < nMaxoResults ? maxoTermScoreRecords.size() : nMaxoResults;
            model.addAttribute("nDisplayed", nDisplayed);
            for (MaxoTermMap.MaxoTermScore maxoTermScoreRecord : maxoTermScoreRecords.subList(0, nDisplayed)) {
                List<MaxoTermMap.Frequencies> frequencyRecords = differentialDiagnosisService.getFrequencyRecords(maxoTermMap, maxoTermScoreRecord);
                maxoTables.put(maxoTermScoreRecord, frequencyRecords);
            }
            model.addAttribute("maxoTables", maxoTables);
        }
        return "differentialDiagnosis";
    }



}
