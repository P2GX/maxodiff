package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.io.PhenopacketFileParser;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.html.analysis.InputRecord;
import org.monarchinitiative.maxodiff.html.service.SessionResultsService;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Controller("/sessionResults")
@SessionAttributes({"inputRecord"})
public class SessionResultsController {

    @Autowired
    SessionResultsService sessionResultsService;

    private final BiometadataService biometadataService;

    private final DiffDiagRefiner diffDiagRefiner;

    public SessionResultsController(
            BiometadataService biometadataService,
            DiffDiagRefiner diffDiagRefiner) {
        this.biometadataService = biometadataService;
        this.diffDiagRefiner = diffDiagRefiner;
    }

    @RequestMapping("/sessionResults")
    public String showResults(@SessionAttribute("inputRecord") InputRecord input,
                             @RequestParam(value = "nDiseases", required = false) Integer nDiseases,
                             @RequestParam(value = "weight", required = false) Double weight,
                             @RequestParam(value = "nMaxoResults", required = false) Integer nMaxoResults,
                             Model model) throws Exception {

        assert input != null;
        Path phenopacketPath = input.phenopacketPath();
        PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(phenopacketPath);
        Sample sample = Sample.of(phenopacketData.sampleId(),
                phenopacketData.presentHpoTermIds().toList(),
                phenopacketData.excludedHpoTermIds().toList());
        AnalysisResults liricalResults = input.liricalResults();
        List<TestResult> liricalResultsList = liricalResults.resultsWithDescendingPostTestProbability().toList();
        List<DifferentialDiagnosis> differentialDiagnoses = liricalResultsList.stream().map(result -> DifferentialDiagnosis.of(result.diseaseId(),
                result.posttestProbability(), result.getCompositeLR())).collect(Collectors.toCollection(LinkedList::new));
        int nLiricalResults = 10;  // TODO: this should not be hard-coded
        model.addAttribute("nLiricalResults", nLiricalResults);
        model.addAttribute("differentialDiagnoses", differentialDiagnoses.subList(0, nLiricalResults));
        model.addAttribute("totalNDiseases", differentialDiagnoses.size());
        model.addAttribute("nDiseases", nDiseases);
        model.addAttribute("weight", weight);
        model.addAttribute("nMaxoResults", nMaxoResults);

        if (phenopacketPath != null & nDiseases != null & weight != null & nMaxoResults != null) {
            RefinementOptions options = RefinementOptions.of(nDiseases, weight);
            RefinementResults refinementResults = diffDiagRefiner.run(sample, differentialDiagnoses, options);
            List<MaxodiffResult> resultsList = new ArrayList<>(refinementResults.maxodiffResults());
            resultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.maxoTermScore().scoreDiff()).reversed());
            TermId diseaseId = phenopacketData.diseaseIds().get(0);
            String phenopacketName = phenopacketPath.toFile().getName();
            model.addAttribute("phenopacket", phenopacketName);
            model.addAttribute("diseaseId", diseaseId);
            model.addAttribute("diseaseLabel", biometadataService.diseaseLabel(diseaseId).orElse("unknown"));
            model.addAttribute("maxodiffResults", resultsList);

            Set<TermId> omimIds = resultsList.get(0).maxoTermScore().omimTermIds();
            model.addAttribute("omimIds", omimIds);

            int nDisplayed = Math.min(resultsList.size(), nMaxoResults);
            model.addAttribute("nDisplayed", nDisplayed);

            Map<String, String> maxoTermsMap = new HashMap<>();
            Map<TermId, String> hpoTermsMap = new HashMap<>();
            Map<TermId, String> diseaseTermsMap = new LinkedHashMap<>();

            for (MaxodiffResult maxodiffResult : resultsList.subList(0, nDisplayed)) {
                MaxoTermScore maxoTermScore = maxodiffResult.maxoTermScore();
                maxoTermsMap.put(maxoTermScore.maxoId(), biometadataService.maxoLabel(maxoTermScore.maxoId()).orElse("unknown"));
                maxoTermScore.hpoTermIds().forEach(id -> hpoTermsMap.put(id, biometadataService.hpoLabel(id).orElse("unknown")));
                maxoTermScore.omimTermIds().forEach(id -> diseaseTermsMap.put(id, biometadataService.diseaseLabel(id).orElse("unknown")));
            }
            model.addAttribute("omimTerms", diseaseTermsMap);
            model.addAttribute("allHpoTermsMap", hpoTermsMap);
            model.addAttribute("allMaxoTermsMap", maxoTermsMap);
            model.addAttribute("maxoTables", resultsList.subList(0, nDisplayed));
        }
        return "sessionResults";
    }



}
