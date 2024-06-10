package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.analysis.TestResult;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.io.PhenopacketFileParser;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.html.analysis.InputRecord;
import org.monarchinitiative.maxodiff.html.service.SessionResultsService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
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
        PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(phenopacketPath);
        Sample sample = Sample.of(phenopacketData.sampleId(),
                phenopacketData.presentHpoTermIds().toList(),
                phenopacketData.excludedHpoTermIds().toList());
        AnalysisResults liricalResults = input.liricalResults();
        List<TestResult> liricalResultsList = liricalResults.resultsWithDescendingPostTestProbability().toList();
        List<DifferentialDiagnosis> differentialDiagnoses = new LinkedList<>();
        for (TestResult result : liricalResultsList) {
            differentialDiagnoses.add(DifferentialDiagnosis.of(result.diseaseId(),
                    result.posttestProbability(), result.getCompositeLR()));
        }
        int nLiricalResults = 10;  // TODO: this should not be hard-coded
        model.addAttribute("nLiricalResults", nLiricalResults);
        model.addAttribute("differentialDiagnoses", differentialDiagnoses.subList(0, nLiricalResults));
        model.addAttribute("totalNDiseases", differentialDiagnoses.size());
        model.addAttribute("nDiseases", nDiseases);
        model.addAttribute("weight", weight);
        model.addAttribute("nMaxoResults", nMaxoResults);
        if (phenopacketPath != null & nDiseases != null & weight != null & nMaxoResults != null) {
            InputRecord inputRecord = (InputRecord) model.getAttribute("inputRecord");
            assert inputRecord != null;
            // TODO: use Biometadata service instead of `MaxoTermMap`
            MaxoTermMap maxoTermMap = inputRecord.maxoTermMap();
            Map<SimpleTerm, Set<SimpleTerm>> fullHpoToMaxoTermMap = maxoTermMap.getFullHpoToMaxoTermMap();
            HpoDiseases diseases = maxoTermMap.getDiseases();
            Map<TermId, Set<TermId>> fullHpoToMaxoTermIdMap = maxoTermMap.getFullHpoToMaxoTermIdMap(fullHpoToMaxoTermMap);
            MinimalOntology hpo = maxoTermMap.getOntology();

            Map<String, String> allMaxoTermsMap = sessionResultsService.getAllMaxoTermsMap(fullHpoToMaxoTermMap);
            model.addAttribute("allMaxoTermsMap", allMaxoTermsMap);
            Map<TermId, String> allHpoTermsMap = sessionResultsService.getAllHpoTermsMap(fullHpoToMaxoTermMap);
            model.addAttribute("allHpoTermsMap", allHpoTermsMap);

            MaxoDiffRefiner maxoDiffRefiner = sessionResultsService.getMaxoDiffRefiner(diseases, fullHpoToMaxoTermIdMap, hpo);
            RefinementOptions options = RefinementOptions.of(nDiseases, weight);
            RefinementResults refinementResults = maxoDiffRefiner.run(sample, differentialDiagnoses, options);
            List<MaxodiffResult> resultsList = refinementResults.maxodiffResults().stream().toList();
            TermId diseaseId = phenopacketData.diseaseIds().get(0);
            String phenopacketName = phenopacketPath.toFile().getName();
            model.addAttribute("phenopacket", phenopacketName);
            model.addAttribute("diseaseId", diseaseId);
            model.addAttribute("maxodiffResults", resultsList);

            Set<TermId> omimIds = resultsList.get(0).maxoTermScore().omimTermIds();
            model.addAttribute("omimIds", omimIds);

            Map<MaxoTermScore, List<Frequencies>> maxoTables = new LinkedHashMap<>();
            int nDisplayed = Math.min(resultsList.size(), nMaxoResults);
            model.addAttribute("nDisplayed", nDisplayed);
            for (MaxodiffResult maxodiffResult : resultsList.subList(0, nDisplayed)) {
                maxoTables.put(maxodiffResult.maxoTermScore(), maxodiffResult.frequencies());
            }
            model.addAttribute("maxoTables", maxoTables);
        }
        return "sessionResults";
    }



}
