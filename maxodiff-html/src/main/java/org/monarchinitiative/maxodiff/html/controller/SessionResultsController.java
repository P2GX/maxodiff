package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Stream;

@Controller("/sessionResults")
@SessionAttributes({"engineName", "sample", "differentialDiagnoses", "nDiseases",
        "hpoTermCounts", "maxoToHpoTermIdMap", "maxoTermToDifferentialDiagnosesMap"})
public class SessionResultsController {

//    @Autowired
//    SessionResultsService sessionResultsService;

    private final BiometadataService biometadataService;

    private DiffDiagRefiner diffDiagRefiner;

    private final MinimalOntology hpo;

    private final HpoDiseases hpoDiseases;

    private final Map<TermId, Set<TermId>> hpoToMaxoIdMap;

    public SessionResultsController(
            BiometadataService biometadataService,
            DiffDiagRefiner diffDiagRefiner,
            MinimalOntology hpo,
            HpoDiseases hpoDiseases,
            Map<TermId, Set<TermId>> hpoToMaxoIdMap
    ) {
        this.biometadataService = biometadataService;
        this.diffDiagRefiner = diffDiagRefiner;
        this.hpo = hpo;
        this.hpoDiseases = hpoDiseases;
        this.hpoToMaxoIdMap = hpoToMaxoIdMap;
    }

    @RequestMapping("/sessionResults")
    public String showResults(@SessionAttribute(value = "sample", required = false) Sample sample,
                              @SessionAttribute(value = "differentialDiagnoses", required = false) List<DifferentialDiagnosis> differentialDiagnoses,
                              @SessionAttribute(value = "engine", required = false) DifferentialDiagnosisEngine engine,
                              @RequestParam(value = "refiner", required = false) String refiner,
                              @RequestParam(value = "nDiseases", required = false) Integer nDiseases,
                              @RequestParam(value = "weight", required = false) Double weight,
                              @RequestParam(value = "nMaxoResults", required = false) Integer nMaxoResults,
                              Model model) {

        String algorithm = "";
        if (refiner == null) {
            refiner = "score";
            algorithm = "Score";
        }

        switch (refiner) {
            case "score" -> {diffDiagRefiner = new MaxoDiffRefiner(hpoDiseases, hpoToMaxoIdMap, hpo);
                            algorithm = "Score";}
            case "rank" -> {diffDiagRefiner = new MaxoDiffRankRefiner(hpoDiseases, hpoToMaxoIdMap, hpo);
                            algorithm = "Rank";}
            case "ddScore" -> {diffDiagRefiner = new MaxoDiffDDScoreRefiner(hpoDiseases, hpoToMaxoIdMap, hpo);
                                algorithm = "Differential Diagnosis Score";}
            case "ksTest" -> {diffDiagRefiner = new MaxoDiffKolmogorovSmirnovRefiner(hpoDiseases, hpoToMaxoIdMap, hpo);
                                algorithm = "Kolomogorov-Smirnov Test";}
        }

        model.addAttribute("refiner", refiner);
        model.addAttribute("algorithm", algorithm);
        Integer prevNDiseases = (Integer) model.getAttribute("nDiseases");
        model.addAttribute("nDiseases", nDiseases);
        model.addAttribute("weight", weight);
        model.addAttribute("nMaxoResults", nMaxoResults);

        if (differentialDiagnoses != null && !differentialDiagnoses.isEmpty()) {
            int nOrigDiffDiagnosesShown = Math.min(differentialDiagnoses.size(), 10);  // TODO: this should not be hard-coded
            model.addAttribute("nOrigDiffDiagnosesShown", nOrigDiffDiagnosesShown);
            model.addAttribute("totalNDiseases", differentialDiagnoses.size());
        }

        if (sample != null && nDiseases != null && weight != null && nMaxoResults != null) {
            RefinementOptions options = RefinementOptions.of(nDiseases, weight);

            if (model.getAttribute("orderedDiagnoses") == null || !nDiseases.equals(prevNDiseases)
                    || diffDiagRefiner instanceof MaxoDiffKolmogorovSmirnovRefiner) {
                List<DifferentialDiagnosis> orderedDiagnoses = diffDiagRefiner.getOrderedDiagnoses(differentialDiagnoses, options);
                model.addAttribute("orderedDiagnoses", orderedDiagnoses);
            }
            List<DifferentialDiagnosis> orderedDiagnoses = (List<DifferentialDiagnosis>) model.getAttribute("orderedDiagnoses");

            if (model.getAttribute("hpoTermCounts") == null || !nDiseases.equals(prevNDiseases)
                    || diffDiagRefiner instanceof MaxoDiffKolmogorovSmirnovRefiner) {
                List<HpoDisease> diseases = diffDiagRefiner.getDiseases(orderedDiagnoses);
                Map<TermId, List<HpoFrequency>> hpoTermCounts = diffDiagRefiner.getHpoTermCounts(diseases);
                model.addAttribute("hpoTermCounts", hpoTermCounts);
            }
            Map<TermId, List<HpoFrequency>> hpoTermCounts = (Map<TermId, List<HpoFrequency>>) model.getAttribute("hpoTermCounts");

            if (model.getAttribute("maxoToHpoTermIdMap") == null || !nDiseases.equals(prevNDiseases)
                    || diffDiagRefiner instanceof MaxoDiffKolmogorovSmirnovRefiner) {
                List<TermId> termIdsToRemove = Stream.of(sample.presentHpoTermIds(), sample.excludedHpoTermIds())
                        .flatMap(Collection::stream).toList();
                Map<TermId, Set<TermId>> maxoToHpoTermIdMap = diffDiagRefiner.getMaxoToHpoTermIdMap(termIdsToRemove, hpoTermCounts);
                model.addAttribute("maxoToHpoTermIdMap", maxoToHpoTermIdMap);
            }
            Map<TermId, Set<TermId>> maxoToHpoTermIdMap = (Map<TermId, Set<TermId>>) model.getAttribute("maxoToHpoTermIdMap");

            if ((model.getAttribute("maxoTermToDifferentialDiagnosesMap") == null || !nDiseases.equals(prevNDiseases)
                    || diffDiagRefiner instanceof MaxoDiffKolmogorovSmirnovRefiner) && !(diffDiagRefiner instanceof MaxoDiffRefiner)) {
                Integer nMapDiseases = diffDiagRefiner instanceof MaxoDiffKolmogorovSmirnovRefiner ? 100 : options.nDiseases();
                Map<TermId, List<DifferentialDiagnosis>> maxoTermToDifferentialDiagnosesMap = diffDiagRefiner.getMaxoTermToDifferentialDiagnosesMap(sample,
                        engine, maxoToHpoTermIdMap, nMapDiseases);
                model.addAttribute("maxoTermToDifferentialDiagnosesMap", maxoTermToDifferentialDiagnosesMap);
            }
            Map<TermId, List<DifferentialDiagnosis>> maxoTermToDDEngineDiagnosesMap = (Map<TermId, List<DifferentialDiagnosis>>) model.getAttribute("maxoTermToDifferentialDiagnosesMap");

            RefinementResults refinementResults = diffDiagRefiner.run(sample,
                                                                      orderedDiagnoses,
                                                                      options,
                                                                      engine,
                                                                      maxoToHpoTermIdMap,
                                                                      hpoTermCounts,
                                                                      maxoTermToDDEngineDiagnosesMap);

            List<MaxodiffResult> resultsList = new ArrayList<>(refinementResults.maxodiffResults());
            if (diffDiagRefiner instanceof MaxoDiffKolmogorovSmirnovRefiner) {
                resultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.maxoTermScore().scoreDiff()));
            } else {
                resultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.maxoTermScore().scoreDiff()).reversed());
            }

            model.addAttribute("maxodiffResults", resultsList);

            Set<TermId> omimIds = resultsList.get(0).maxoTermScore().omimTermIds();
            model.addAttribute("omimIds", omimIds);
            Set<TermId> maxoOmimIds = resultsList.get(0).maxoTermScore().maxoOmimTermIds();
            model.addAttribute("maxoOmimIds", maxoOmimIds);

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
                maxoTermScore.maxoOmimTermIds().forEach(id -> diseaseTermsMap.put(id, biometadataService.diseaseLabel(id).orElse("unknown")));
            }
            model.addAttribute("omimTerms", diseaseTermsMap);
            model.addAttribute("allHpoTermsMap", hpoTermsMap);
            model.addAttribute("allMaxoTermsMap", maxoTermsMap);
            model.addAttribute("maxoTables", resultsList.subList(0, nDisplayed));
        }
        return "sessionResults";
    }



}
