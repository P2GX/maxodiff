package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermMap;
import org.monarchinitiative.maxodiff.html.analysis.InputRecord;
import org.monarchinitiative.maxodiff.html.service.SessionResultsService;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.*;

@Controller("/sessionResults")
@SessionAttributes({"liricalRecord", "inputRecord"})
public class SessionResultsController {

    @Autowired
    SessionResultsService sessionResultsService;

    @RequestMapping("/sessionResults")
    public String showResults(@SessionAttribute("inputRecord") InputRecord input,
                             @RequestParam(value = "nDiseases", required = false) Integer nDiseases,
                             @RequestParam(value = "weight", required = false) Double weight,
                             @RequestParam(value = "nMaxoResults", required = false) Integer nMaxoResults,
                             Model model) throws Exception {

        Path phenopacketPath = input.phenopacketPath();
        AnalysisResults liricalResults = input.liricalResults();
        List<TestResult> liricalResultsList = liricalResults.resultsWithDescendingPostTestProbability().toList();
        int nLiricalResults = 10;
        model.addAttribute("nLiricalResults", nLiricalResults);
        model.addAttribute("liricalResultsList", liricalResultsList.subList(0, nLiricalResults));
        model.addAttribute("totalNDiseases", liricalResultsList.size());
        model.addAttribute("nDiseases", nDiseases);
        model.addAttribute("weight", weight);
        model.addAttribute("nMaxoResults", nMaxoResults);
        if (phenopacketPath != null & nDiseases != null & weight != null & nMaxoResults != null) {
            InputRecord inputRecord = (InputRecord) model.getAttribute("inputRecord");
            MaxoTermMap maxoTermMap = inputRecord.maxoTermMap();
            List<MaxoTermMap.MaxoTermScore> maxoTermScoreRecords = sessionResultsService.getMaxoTermRecords(maxoTermMap, liricalResults, phenopacketPath, nDiseases, weight);
            TermId diseaseId = sessionResultsService.getDiseaseId(maxoTermMap);
            String phenopacketName = phenopacketPath.toFile().getName();
            model.addAttribute("phenopacket", phenopacketName);
            model.addAttribute("diseaseId", diseaseId);
            model.addAttribute("maxoRecords", maxoTermScoreRecords);

            Set<TermId> omimIds = maxoTermScoreRecords.get(0).omimTermIds();
            model.addAttribute("omimIds", omimIds);

            Map<MaxoTermMap.MaxoTermScore, List<MaxoTermMap.Frequencies>> maxoTables = new LinkedHashMap<>();
            int nDisplayed = maxoTermScoreRecords.size() < nMaxoResults ? maxoTermScoreRecords.size() : nMaxoResults;
            model.addAttribute("nDisplayed", nDisplayed);
            for (MaxoTermMap.MaxoTermScore maxoTermScoreRecord : maxoTermScoreRecords.subList(0, nDisplayed)) {
                List<MaxoTermMap.Frequencies> frequencyRecords = sessionResultsService.getFrequencyRecords(maxoTermMap, maxoTermScoreRecord);
                maxoTables.put(maxoTermScoreRecord, frequencyRecords);
            }
            model.addAttribute("maxoTables", maxoTables);
        }
        return "sessionResults";
    }



}
