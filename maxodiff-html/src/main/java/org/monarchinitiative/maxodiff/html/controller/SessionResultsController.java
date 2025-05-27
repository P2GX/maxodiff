package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbabilities;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.analysis.refinement.*;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.html.results.HtmlResults;
import org.monarchinitiative.maxodiff.lirical.LiricalDifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.RankMaxo;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.lirical.LiricalDifferentialDiagnosisEngineConfigurer;
import org.monarchinitiative.maxodiff.phenomizer.PhenomizerDifferentialDiagnosisEngine;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller("/sessionResults")
@SessionAttributes({"engineName", "sample", "differentialDiagnoses", "nDiseases",
        "hpoTermCounts", "maxoToHpoTermIdMap", "maxoTermToDifferentialDiagnosesMap"})
public class SessionResultsController {

//    @Autowired
//    SessionResultsService sessionResultsService;

    private final BiometadataService biometadataService;

    private DiffDiagRefiner diffDiagRefiner;

    private final MinimalOntology minHpo;

    private final Ontology hpo;

    private final HpoDiseases hpoDiseases;

    private final Map<TermId, Set<TermId>> hpoToMaxoIdMap;

    private final Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap;

    public SessionResultsController(
            BiometadataService biometadataService,
            DiffDiagRefiner diffDiagRefiner,
            MinimalOntology minHpo,
            Ontology hpo,
            HpoDiseases hpoDiseases,
            Map<TermId, Set<TermId>> hpoToMaxoIdMap,
            Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap
    ) {
        this.biometadataService = biometadataService;
        this.diffDiagRefiner = diffDiagRefiner;
        this.minHpo = minHpo;
        this.hpo = hpo;
        this.hpoDiseases = hpoDiseases;
        this.hpoToMaxoIdMap = hpoToMaxoIdMap;
        this.hpoToMaxoTermMap = hpoToMaxoTermMap;
    }

    @RequestMapping("/sessionResults")
    public String showResults(@SessionAttribute(value = "sample", required = false) Sample sample,
                              @SessionAttribute(value = "differentialDiagnoses", required = false) List<DifferentialDiagnosis> differentialDiagnoses,
                              @SessionAttribute(value = "engine", required = false) DifferentialDiagnosisEngine engine,
                              @SessionAttribute(value = "liricalEngineConfigurer", required = false) LiricalDifferentialDiagnosisEngineConfigurer liricalDifferentialDiagnosisEngineConfigurer,
                              @RequestParam(value = "refiner", required = false) String refiner,
                              @RequestParam(value = "nDiseases", required = false) Integer nDiseases,
                              @RequestParam(value = "nRepetitions", required = false) Integer nRepetitions,
                              @RequestParam(value = "nMaxoResults", required = false) Integer nMaxoResults,
                              @RequestParam(value = "diseaseProbModel", required = false) String diseaseProbModel,
                              Model model) throws Exception {

        String algorithm = "";
        if (refiner == null) {
            refiner = "score";
            algorithm = "Score";
        }

        if (refiner.equals("score")) {
            diffDiagRefiner = new MaxoDiffRefiner(hpoDiseases, hpoToMaxoIdMap, hpoToMaxoTermMap, minHpo, hpo);
            algorithm = "Score";
        }

        model.addAttribute("refiner", refiner);
        model.addAttribute("algorithm", algorithm);
        Integer prevNDiseases = (Integer) model.getAttribute("nDiseases");
        model.addAttribute("nDiseases", nDiseases);
        model.addAttribute("nRepetitions", nRepetitions);
        model.addAttribute("nMaxoResults", nMaxoResults);
        model.addAttribute("diseaseProbModel", diseaseProbModel);

        if (differentialDiagnoses != null && !differentialDiagnoses.isEmpty()) {
            int nOrigDiffDiagnosesShown = Math.min(differentialDiagnoses.size(), 10);  // TODO: this should not be hard-coded
            model.addAttribute("nOrigDiffDiagnosesShown", nOrigDiffDiagnosesShown);
            model.addAttribute("totalNDiseases", differentialDiagnoses.size());
        }

        if (sample != null && nDiseases != null && nRepetitions != null && nMaxoResults != null) {
            RefinementOptions options = RefinementOptions.of(nDiseases, nRepetitions);

            if (model.getAttribute("orderedDiagnoses") == null || !nDiseases.equals(prevNDiseases)) {
                List<DifferentialDiagnosis> orderedDiagnoses = diffDiagRefiner.getOrderedDiagnoses(differentialDiagnoses, options);
                model.addAttribute("orderedDiagnoses", orderedDiagnoses);
            }
            List<DifferentialDiagnosis> orderedDiagnoses = (List<DifferentialDiagnosis>) model.getAttribute("orderedDiagnoses");

            if (model.getAttribute("hpoTermCounts") == null || !nDiseases.equals(prevNDiseases)) {
                List<HpoDisease> diseases = diffDiagRefiner.getDiseases(orderedDiagnoses);
                Map<TermId, List<HpoFrequency>> hpoTermCounts = diffDiagRefiner.getHpoTermCounts(diseases);
                model.addAttribute("hpoTermCounts", hpoTermCounts);
            }
            Map<TermId, List<HpoFrequency>> hpoTermCounts = (Map<TermId, List<HpoFrequency>>) model.getAttribute("hpoTermCounts");

            if (model.getAttribute("maxoToHpoTermIdMap") == null || !nDiseases.equals(prevNDiseases)) {
                List<TermId> termIdsToRemove = Stream.of(sample.presentHpoTermIds(), sample.excludedHpoTermIds())
                        .flatMap(Collection::stream).toList();
                Map<TermId, Set<TermId>> maxoToHpoTermIdMap = diffDiagRefiner.getMaxoToHpoTermIdMap(termIdsToRemove, hpoTermCounts);
                model.addAttribute("maxoToHpoTermIdMap", maxoToHpoTermIdMap);
            }
            Map<TermId, Set<TermId>> maxoToHpoTermIdMap = (Map<TermId, Set<TermId>>) model.getAttribute("maxoToHpoTermIdMap");

            RefinementResults refinementResults = null;
            DifferentialDiagnosisEngine diseaseSubsetEngine = null;
            if (diffDiagRefiner instanceof MaxoDiffRefiner) {
                assert orderedDiagnoses != null;
                List<DifferentialDiagnosis> initialDiagnoses = orderedDiagnoses.stream().toList()
                        .subList(0, options.nDiseases());

                if (engine instanceof LiricalDifferentialDiagnosisEngine) {
                    Set<TermId> initialDiagnosesIds = initialDiagnoses.stream()
                            .map(DifferentialDiagnosis::diseaseId)
                            .collect(Collectors.toSet());

                    AnalysisOptions originalOptions = ((LiricalDifferentialDiagnosisEngine) engine).getAnalysisOptions();

                    var diseaseSubsetOptions = AnalysisOptions.builder()
                            .useStrictPenalties(originalOptions.useStrictPenalties())
                            .useGlobal(originalOptions.useGlobal())
                            .pretestProbability(PretestDiseaseProbabilities.uniform(initialDiagnosesIds))
                            .addTargetDiseases(initialDiagnosesIds)
                            .build();
                    diseaseSubsetEngine = liricalDifferentialDiagnosisEngineConfigurer.configure(diseaseSubsetOptions);
                } else if (engine instanceof PhenomizerDifferentialDiagnosisEngine) {
                    diseaseSubsetEngine = engine;
                }
                assert maxoToHpoTermIdMap != null;
                RankMaxo rankMaxo = ((MaxoDiffRefiner) diffDiagRefiner).getRankMaxo(initialDiagnoses,
                        diseaseSubsetEngine,
                        maxoToHpoTermIdMap,
                        diseaseProbModel);
                model.addAttribute("progress", rankMaxo.updateProgress());
                refinementResults = diffDiagRefiner.run(sample,
                        orderedDiagnoses,
                        options,
                        rankMaxo,
                        hpoTermCounts,
                        maxoToHpoTermIdMap);
            }


            List<MaxodiffResult> resultsList = new ArrayList<>(refinementResults.maxodiffResults());
            if (diffDiagRefiner instanceof MaxoDiffRefiner) {
                resultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.rankMaxoScore().maxoScore()).reversed());
            }

            model.addAttribute("maxodiffResults", resultsList);

            int nDisplayed = Math.min(resultsList.size(), nMaxoResults);
            model.addAttribute("nDisplayed", nDisplayed);

            StringBuilder samplePresentTermsStringBuilder = new StringBuilder();
            sample.presentHpoTermIds().forEach(tid -> samplePresentTermsStringBuilder
                    .append(biometadataService.hpoLabel(tid).orElse("unknown")).append(" (")
                    .append(tid).append("), "));
            String samplePresentTermsString = samplePresentTermsStringBuilder.substring(0, samplePresentTermsStringBuilder.length()-2);
            StringBuilder sampleExcludedTermsStringBuilder = new StringBuilder();
            sample.excludedHpoTermIds().forEach(tid -> sampleExcludedTermsStringBuilder
                    .append(biometadataService.hpoLabel(tid).orElse("unknown")).append(" (")
                    .append(tid).append("), "));
            String sampleExcludedTermsString = sampleExcludedTermsStringBuilder.substring(0, sampleExcludedTermsStringBuilder.length()-2);
            model.addAttribute("samplePresentTermsString", samplePresentTermsString);
            model.addAttribute("sampleExcludedTermsString", sampleExcludedTermsString);

            Map<String, String> maxoTermsMap = new HashMap<>();
            Map<TermId, String> hpoTermsMap = new HashMap<>();
            Map<TermId, String> diseaseTermsMap = new LinkedHashMap<>();

            List<HpoFrequency> hpoFrequencies = HTMLFrequencyMap.getHpoFrequencies(hpoTermCounts);
            Map<TermId, Integer> nRepetitionsMap = new HashMap<>();
            Map<String, Map<Float, List<String>>> frequencyMap = new HashMap<>();

            for (MaxodiffResult maxodiffResult : resultsList.subList(0, nDisplayed)) {
                if (diffDiagRefiner instanceof MaxoDiffRefiner) {
                    RankMaxoScore rankMaxoScore = maxodiffResult.rankMaxoScore();
                    maxoTermsMap.put(rankMaxoScore.maxoId().toString(), biometadataService.maxoLabel(rankMaxoScore.maxoId().toString()).orElse("unknown"));
                    rankMaxoScore.discoverableObservedHpoTermIds().forEach(id -> hpoTermsMap.put(id, biometadataService.hpoLabel(id).orElse("unknown")));
                    rankMaxoScore.initialOmimTermIds().forEach(id -> diseaseTermsMap.put(id, biometadataService.diseaseLabel(id).orElse("unknown")));
                    rankMaxoScore.maxoOmimTermIds().forEach(id -> diseaseTermsMap.put(id, biometadataService.diseaseLabel(id).orElse("unknown")));
                    var hpoTermIdRepCtsMap = rankMaxoScore.hpoTermIdRepCtsMap();
                    for (Map.Entry<TermId, Map<TermId, Integer>> diseaseHpoRepCtEntry : hpoTermIdRepCtsMap.entrySet()) {
                        Map<TermId, Integer> hpoRetCtMap = diseaseHpoRepCtEntry.getValue();
                        for (Map.Entry<TermId, Integer> hpoRepCtMapEntry : hpoRetCtMap.entrySet()) {
                            TermId hpoId = hpoRepCtMapEntry.getKey();
                            Integer repCt = hpoRepCtMapEntry.getValue();
                            if (repCt != null && !nRepetitionsMap.containsKey(hpoId)) {
                                nRepetitionsMap.put(hpoId, repCt);
                                break;
                            }
                        }
                    }
                    Map<String, Map<Float, List<String>>> resultFrequencyMap = HTMLFrequencyMap.makeFrequencyDiseaseMap(hpoTermsMap, diseaseTermsMap, hpoTermIdRepCtsMap, hpoFrequencies);
                    frequencyMap.putAll(resultFrequencyMap);
                }

            }
            model.addAttribute("omimTerms", diseaseTermsMap);
            model.addAttribute("nRepetitionsMap", nRepetitionsMap);
            model.addAttribute("frequencyDiseaseMap", frequencyMap);
            model.addAttribute("allHpoTermsMap", hpoTermsMap);
            model.addAttribute("allMaxoTermsMap", maxoTermsMap);
            model.addAttribute("maxoTables", resultsList.subList(0, nDisplayed));

            String htmlString = HtmlResults.writeHTMLResults(sample, nDiseases, nRepetitions, resultsList,
                    biometadataService, hpoTermCounts);

            File file = new File("maxodiff-html-results/src/main/resources/templates/maxodiffResults.html");
            String htmlTemplatePath = file.getAbsolutePath();

            model.addAttribute("htmlTemplatePath", htmlTemplatePath);
            model.addAttribute("htmlTemplateString", htmlString);
        }
        return "sessionResults";
    }



}
