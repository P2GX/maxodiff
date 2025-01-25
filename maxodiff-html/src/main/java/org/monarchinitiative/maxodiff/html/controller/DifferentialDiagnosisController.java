package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementOptions;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementResults;
import org.monarchinitiative.maxodiff.core.io.LiricalResultsFileParser;
import org.monarchinitiative.maxodiff.core.io.PhenopacketFileParser;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Controller("/")
public class DifferentialDiagnosisController {

    private final BiometadataService biometadataService;

    private final DiffDiagRefiner diffDiagRefiner;

    public DifferentialDiagnosisController(
            BiometadataService biometadataService,
            DiffDiagRefiner diffDiagRefiner
    ) {
        this.biometadataService = biometadataService;
        this.diffDiagRefiner = diffDiagRefiner;
    }

    // TODO: How can we get an object like `Sample` through the wire?
    // It may be easier to receive a class instead of an interface..

    @RequestMapping("/")
    public String getInput(
            // Sample sample,
            // A list of differential diagnoses, not necessarily the LIRICAL stuff
            @RequestParam(value = "phenopacketPath", required = false) Path phenopacketPath,
            @RequestParam(value = "liricalResultsPath", required = false) Path liricalResultsPath,
            // TODO: make the values below mandatory
            @RequestParam(value = "nDiseases", required = false) Integer nDiseases,
            @RequestParam(value = "weight", required = false) Double weight,
            @RequestParam(value = "nMaxoResults", required = false) Integer nMaxoResults,
            Model model) throws Exception {

        model.addAttribute("nDiseases", nDiseases);
        model.addAttribute("weight", weight);
        model.addAttribute("nMaxoResults", nMaxoResults);

        model.addAttribute("phenopacketPath", phenopacketPath);
        model.addAttribute("liricalResultsPath", liricalResultsPath);
        int nLiricalResults = 10; // At some point make this either a "global" configuration, or a user input
        model.addAttribute("nLiricalResults", nLiricalResults);
        List<DifferentialDiagnosis> differentialDiagnoses = null;
        if (liricalResultsPath != null) {
            differentialDiagnoses = LiricalResultsFileParser.read(liricalResultsPath);
            model.addAttribute("totalNDiseases", differentialDiagnoses.size());
            model.addAttribute("differentialDiagnoses", differentialDiagnoses.subList(0, nLiricalResults));
        }
        Map<TermId, Double> initialScores = new LinkedHashMap<>();
        if (differentialDiagnoses != null)
            differentialDiagnoses.subList(0, nDiseases).forEach(d -> initialScores.put(d.diseaseId(), d.score()));
        model.addAttribute("initialScores", initialScores);

        /*
         - We should work on the level of sample, not phenopacket.
         */

        if (phenopacketPath != null & differentialDiagnoses != null) {
            PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(phenopacketPath);
            Sample sample = Sample.of(phenopacketData.sampleId(),
                    phenopacketData.presentHpoTermIds().toList(),
                    phenopacketData.excludedHpoTermIds().toList());
            RefinementOptions options = RefinementOptions.of(nDiseases, weight);

            List<DifferentialDiagnosis> orderedDiagnoses = null;
            if (model.getAttribute("hpoTermCounts") == null) {
                orderedDiagnoses = diffDiagRefiner.getOrderedDiagnoses(differentialDiagnoses, options);
                List<HpoDisease> diseases = diffDiagRefiner.getDiseases(orderedDiagnoses);
                Map<TermId, List<HpoFrequency>> hpoTermCounts = diffDiagRefiner.getHpoTermCounts(diseases);
                model.addAttribute("hpoTermCounts", hpoTermCounts);
            }
            Map<TermId, List<HpoFrequency>> hpoTermCounts = (Map<TermId, List<HpoFrequency>>) model.getAttribute("hpoTermCounts");

            if (model.getAttribute("maxoToHpoTermIdMap") == null) {
                List<TermId> termIdsToRemove = Stream.of(sample.presentHpoTermIds(), sample.excludedHpoTermIds())
                        .flatMap(Collection::stream).toList();
                Map<TermId, Set<TermId>> maxoToHpoTermIdMap = diffDiagRefiner.getMaxoToHpoTermIdMap(termIdsToRemove, hpoTermCounts);
                model.addAttribute("maxoToHpoTermIdMap", maxoToHpoTermIdMap);
            }
            Map<TermId, Set<TermId>> maxoToHpoTermIdMap = (Map<TermId, Set<TermId>>) model.getAttribute("maxoToHpoTermIdMap");

            RefinementResults results = diffDiagRefiner.run(sample, orderedDiagnoses, options, null, maxoToHpoTermIdMap, hpoTermCounts, null);

            // Show at most n MAxO results
            List<MaxodiffResult> resultsList = new ArrayList<>(results.maxodiffResults());
            resultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.maxoTermScore().scoreDiff()).reversed());
            TermId diseaseId = phenopacketData.diseaseIds().get(0);
            String phenopacketName = phenopacketPath.toFile().getName();
            model.addAttribute("phenopacket", phenopacketName);
            model.addAttribute("diseaseId", diseaseId);
            model.addAttribute("diseaseLabel", biometadataService.diseaseLabel(diseaseId).orElse("unknown"));
            model.addAttribute("maxodiffResults", resultsList);

            int nDisplayed = Math.min(resultsList.size(), nMaxoResults);
            model.addAttribute("nDisplayed", nDisplayed);

            Map<String, String> maxoTermsMap = new HashMap<>();
            Map<TermId, String> hpoTermsMap = new HashMap<>();
            // We MUST use LinkedHashMap to maintain the map order
            // for the header of the frequency table in the HTML report.
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
        return "differentialDiagnosis";
    }


}
