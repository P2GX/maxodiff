package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbabilities;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.HTMLFrequencyMap;
import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.RankMaxoScore;
import org.monarchinitiative.maxodiff.core.analysis.refinement.*;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.RankMaxo;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.html.results.HtmlResults;
import org.monarchinitiative.maxodiff.lirical.LiricalDifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.lirical.LiricalDifferentialDiagnosisEngineConfigurer;
import org.monarchinitiative.maxodiff.lirical.PhenopacketFileParser;
import org.monarchinitiative.maxodiff.phenomizer.IcMicaData;
import org.monarchinitiative.maxodiff.phenomizer.PhenomizerDifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.phenomizer.ScoringMode;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller("/maxodiff")
//@SessionAttributes({"engineName", "differentialDiagnoses", "nDiseases",
//        "hpoTermCounts", "maxoToHpoTermIdMap", "maxoTermToDifferentialDiagnosesMap"})
public class MaxodiffController {

    private final LiricalDifferentialDiagnosisEngineConfigurer liricalDifferentialDiagnosisEngineConfigurer;

    private final IcMicaData icMicaData;

    private final BiometadataService biometadataService;

    private DiffDiagRefiner diffDiagRefiner;

    private final MinimalOntology minHpo;

    private final Ontology hpo;

    private final HpoDiseases hpoDiseases;

    private final Map<TermId, Set<TermId>> hpoToMaxoIdMap;

    private final Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap;

    private RankMaxo rankMaxo;

    public MaxodiffController(
            LiricalDifferentialDiagnosisEngineConfigurer liricalDifferentialDiagnosisEngineConfigurer,
            IcMicaData icMicaData,
            BiometadataService biometadataService,
            DiffDiagRefiner diffDiagRefiner,
            MinimalOntology minHpo,
            Ontology hpo,
            HpoDiseases hpoDiseases,
            Map<TermId, Set<TermId>> hpoToMaxoIdMap,
            Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap
    ) {
        this.icMicaData = icMicaData;
        this.liricalDifferentialDiagnosisEngineConfigurer = liricalDifferentialDiagnosisEngineConfigurer;
        this.biometadataService = biometadataService;
        this.diffDiagRefiner = diffDiagRefiner;
        this.minHpo = minHpo;
        this.hpo = hpo;
        this.hpoDiseases = hpoDiseases;
        this.hpoToMaxoIdMap = hpoToMaxoIdMap;
        this.hpoToMaxoTermMap = hpoToMaxoTermMap;
    }

    @RequestMapping("/maxodiff")
    public String showResults(@RequestParam(value = "engineName", required = false) String engineName,
                              @RequestParam(value = "strict", required = false) boolean strict,
                              @RequestParam(value = "globalAnalysisMode", required = false) boolean globalAnalysisMode,
                              @RequestParam(value = "scoringMode", required = false) ScoringMode scoringMode,
                              @RequestParam(value = "phenopacketPath", required = false) Path phenopacketPath,
                              @RequestParam(value = "id", required = false) String sampleId,
                              @RequestParam(value = "presentHpoTermIds", required = false) String presentHpoTermIds,
                              @RequestParam(value = "excludedHpoTermIds", required = false) String excludedHpoTermIds,
                              @RequestParam(value = "refiner", required = false) String refiner,
                              @RequestParam(value = "nDiseases", required = false) Integer nDiseases,
                              @RequestParam(value = "nRepetitions", required = false) Integer nRepetitions,
                              @RequestParam(value = "nMaxoResults", required = false) Integer nMaxoResults,
                              @RequestParam(value = "diseaseProbModel", required = false) String diseaseProbModel,
                              Model model) throws Exception {

        if (engineName == null) {
            engineName = "lirical";
        }

        model.addAttribute("engineName", engineName);

        model.addAttribute("phenopacketPath", phenopacketPath);
        model.addAttribute("sampleId", sampleId);
        model.addAttribute("presentHpoTermIds", presentHpoTermIds);
        model.addAttribute("excludedHpoTermIds", excludedHpoTermIds);

        if (phenopacketPath != null) {
            PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(phenopacketPath);
            if (sampleId == null | (sampleId != null && sampleId.isEmpty())) {
                sampleId = phenopacketData.sampleId();
            }
            if (presentHpoTermIds == null | (presentHpoTermIds != null && presentHpoTermIds.isEmpty())) {
                presentHpoTermIds = phenopacketData.presentHpoTermIds().map(Object::toString).collect(Collectors.joining(","));
            }
            if (excludedHpoTermIds == null | (excludedHpoTermIds != null && excludedHpoTermIds.isEmpty())) {
                excludedHpoTermIds = phenopacketData.excludedHpoTermIds().map(Object::toString).collect(Collectors.joining(","));
            }
        }

        //TODO: add other possible separators to regex
        //TODO: only add valid termIDs to list
        List<TermId> presentHpoTermIdsList = (presentHpoTermIds == null | (presentHpoTermIds != null && presentHpoTermIds.isEmpty())) ?
                List.of() : Arrays.stream(presentHpoTermIds.split("[\\s,;]+"))
                .map(String::strip)
                .map(TermId::of)
                .toList();
        List<TermId> excludedHpoTermIdsList = (excludedHpoTermIds == null | (excludedHpoTermIds != null && excludedHpoTermIds.isEmpty())) ?
                List.of() : Arrays.stream(excludedHpoTermIds.split("[\\s,;]+"))
                .map(String::strip)
                .map(TermId::of)
                .toList();

        Sample sample = Sample.of(sampleId,
                presentHpoTermIdsList,
                excludedHpoTermIdsList);
        model.addAttribute("sample", sample);

        System.out.println("maxodiff sample = " + sample);

        DifferentialDiagnosisEngine engine = null;
        List<DifferentialDiagnosis> differentialDiagnoses = List.of();

        if (engineName.equals("lirical")) {
            model.addAttribute("strict", strict);
            model.addAttribute("globalAnalysisMode", globalAnalysisMode);

            AnalysisOptions options = AnalysisOptions.builder()
                    .useStrictPenalties(strict)
                    .useGlobal(globalAnalysisMode)
                    .pretestProbability(PretestDiseaseProbabilities.uniform(hpoDiseases.diseaseIds()))
                    .build();

            System.out.println(options);

            engine = liricalDifferentialDiagnosisEngineConfigurer.configure(options);
            model.addAttribute("options", options);

            if (sample != null && sample.id() != null && options != null) {
                // Get initial differential diagnoses from running LIRICAL
                differentialDiagnoses = engine.run(sample);
            }
        } else if (engineName.equals("phenomizer")) {
            if (scoringMode == null) {
                scoringMode = ScoringMode.ONE_SIDED;
            }
            model.addAttribute("scoringMode", scoringMode);

            Map<TermPair, Double> icMicaDict = icMicaData.icMicaDict();
            engine = new PhenomizerDifferentialDiagnosisEngine(hpoDiseases, icMicaDict, scoringMode);

            model.addAttribute("icMicaDict", icMicaDict);

            if (sample != null && sample.id() != null) {
                // Get initial differential diagnoses from running Phenomizer
                differentialDiagnoses = engine.run(sample);
            }
        }
        model.addAttribute("engine", engine);
        model.addAttribute("differentialDiagnoses", differentialDiagnoses);


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

        if (sample != null && differentialDiagnoses != null && nDiseases != null && nRepetitions != null && nMaxoResults != null) {
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
                rankMaxo = ((MaxoDiffRefiner) diffDiagRefiner).getRankMaxo(initialDiagnoses,
                        diseaseSubsetEngine,
                        maxoToHpoTermIdMap,
                        diseaseProbModel);
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
                    rankMaxoScore.remainingHpoTermIds().forEach(id -> hpoTermsMap.put(id, biometadataService.hpoLabel(id).orElse("unknown")));
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
        return "maxodiff";
    }

    @GetMapping("progress1")
    @ResponseBody
    public double getProgress() {
        return rankMaxo.getRankMaxoProgress().getTotalProgress();
    }

    @GetMapping("progress-bar1")
    public String showProgressPage() {
        return "progress1";
    }


    @GetMapping("/updateSample")
    public String updateSample(@RequestParam(value = "engineName", required = false) String engineName,
                               @RequestParam(value = "phenopacketPath", required = false) Path phenopacketPath,
                               @RequestParam(value = "id", required = false) String sampleId,
                               @RequestParam(value = "presentHpoTermIds", required = false) String presentHpoTermIds,
                               @RequestParam(value = "excludedHpoTermIds", required = false) String excludedHpoTermIds,
                               Model model) throws Exception {

        if (engineName == null) {
            engineName = "lirical";
        }

        model.addAttribute("engineName", engineName);

        model.addAttribute("phenopacketPath", phenopacketPath);
        model.addAttribute("sampleId", sampleId);
        model.addAttribute("presentHpoTermIds", presentHpoTermIds);
        model.addAttribute("excludedHpoTermIds", excludedHpoTermIds);

        if (phenopacketPath != null) {
            PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(phenopacketPath);
            if (sampleId == null | (sampleId != null && sampleId.isEmpty())) {
                sampleId = phenopacketData.sampleId();
            }
            if (presentHpoTermIds == null | (presentHpoTermIds != null && presentHpoTermIds.isEmpty())) {
                presentHpoTermIds = phenopacketData.presentHpoTermIds().map(Object::toString).collect(Collectors.joining(","));
            }
            if (excludedHpoTermIds == null | (excludedHpoTermIds != null && excludedHpoTermIds.isEmpty())) {
                excludedHpoTermIds = phenopacketData.excludedHpoTermIds().map(Object::toString).collect(Collectors.joining(","));
            }
        }

        //TODO: add other possible separators to regex
        //TODO: only add valid termIDs to list
        List<TermId> presentHpoTermIdsList = (presentHpoTermIds == null | (presentHpoTermIds != null && presentHpoTermIds.isEmpty())) ?
                List.of() : Arrays.stream(presentHpoTermIds.split("[\\s,;]+"))
                .map(String::strip)
                .map(TermId::of)
                .toList();
        List<TermId> excludedHpoTermIdsList = (excludedHpoTermIds == null | (excludedHpoTermIds != null && excludedHpoTermIds.isEmpty())) ?
                List.of() : Arrays.stream(excludedHpoTermIds.split("[\\s,;]+"))
                .map(String::strip)
                .map(TermId::of)
                .toList();

        Sample sample = Sample.of(sampleId,
                presentHpoTermIdsList,
                excludedHpoTermIdsList);
        model.addAttribute("sample", sample);

        System.out.println("updateSample sample = " + sample);
        System.out.println("updateSample model sample = " + model.getAttribute("sample"));
        System.out.println("updateSample engine = " + engineName);

        return "maxodiff";
    }

    @GetMapping("/updateEngine")
    public String updateEngine(@RequestParam(value = "engineName", required = false) String engineName,
                               @RequestParam(value = "phenopacketPath", required = false) Path phenopacketPath,
                               @RequestParam(value = "id", required = false) String sampleId,
                               @RequestParam(value = "presentHpoTermIds", required = false) String presentHpoTermIds,
                               @RequestParam(value = "excludedHpoTermIds", required = false) String excludedHpoTermIds,
                               Model model) throws Exception {

        if (engineName == null) {
            engineName = "lirical";
        }

        model.addAttribute("engineName", engineName);

        model.addAttribute("phenopacketPath", phenopacketPath);
        model.addAttribute("sampleId", sampleId);
        model.addAttribute("presentHpoTermIds", presentHpoTermIds);
        model.addAttribute("excludedHpoTermIds", excludedHpoTermIds);

        if (phenopacketPath != null) {
            PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(phenopacketPath);
            if (sampleId == null | (sampleId != null && sampleId.isEmpty())) {
                sampleId = phenopacketData.sampleId();
            }
            if (presentHpoTermIds == null | (presentHpoTermIds != null && presentHpoTermIds.isEmpty())) {
                presentHpoTermIds = phenopacketData.presentHpoTermIds().map(Object::toString).collect(Collectors.joining(","));
            }
            if (excludedHpoTermIds == null | (excludedHpoTermIds != null && excludedHpoTermIds.isEmpty())) {
                excludedHpoTermIds = phenopacketData.excludedHpoTermIds().map(Object::toString).collect(Collectors.joining(","));
            }
        }

        //TODO: add other possible separators to regex
        //TODO: only add valid termIDs to list
        List<TermId> presentHpoTermIdsList = (presentHpoTermIds == null | (presentHpoTermIds != null && presentHpoTermIds.isEmpty())) ?
                List.of() : Arrays.stream(presentHpoTermIds.split("[\\s,;]+"))
                .map(String::strip)
                .map(TermId::of)
                .toList();
        List<TermId> excludedHpoTermIdsList = (excludedHpoTermIds == null | (excludedHpoTermIds != null && excludedHpoTermIds.isEmpty())) ?
                List.of() : Arrays.stream(excludedHpoTermIds.split("[\\s,;]+"))
                .map(String::strip)
                .map(TermId::of)
                .toList();

        Sample sample = Sample.of(sampleId,
                presentHpoTermIdsList,
                excludedHpoTermIdsList);
        model.addAttribute("sample", sample);

        System.out.println("updateEngine sample = " + sample);
        System.out.println("updateEngine engine = " + engineName);

        return "maxodiff";
    }

    @GetMapping("/initialDiagnoses")
    public String initialDiagnoses(@RequestParam(value = "engineName", required = false) String engineName,
                                   @RequestParam(value = "phenopacketPath", required = false) Path phenopacketPath,
                                   @RequestParam(value = "id", required = false) String sampleId,
                                   @RequestParam(value = "presentHpoTermIds", required = false) String presentHpoTermIds,
                                   @RequestParam(value = "excludedHpoTermIds", required = false) String excludedHpoTermIds,
                                   @RequestParam(value = "strict", required = false) boolean strict,
                                   @RequestParam(value = "globalAnalysisMode", required = false) boolean globalAnalysisMode,
                                   @RequestParam(value = "scoringMode", required = false) ScoringMode scoringMode,
                                   Model model) throws Exception {

        if (engineName == null) {
            engineName = "lirical";
        }

        model.addAttribute("engineName", engineName);

        model.addAttribute("phenopacketPath", phenopacketPath);
        model.addAttribute("sampleId", sampleId);
        model.addAttribute("presentHpoTermIds", presentHpoTermIds);
        model.addAttribute("excludedHpoTermIds", excludedHpoTermIds);

        if (phenopacketPath != null) {
            PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(phenopacketPath);
            if (sampleId == null | (sampleId != null && sampleId.isEmpty())) {
                sampleId = phenopacketData.sampleId();
            }
            if (presentHpoTermIds == null | (presentHpoTermIds != null && presentHpoTermIds.isEmpty())) {
                presentHpoTermIds = phenopacketData.presentHpoTermIds().map(Object::toString).collect(Collectors.joining(","));
            }
            if (excludedHpoTermIds == null | (excludedHpoTermIds != null && excludedHpoTermIds.isEmpty())) {
                excludedHpoTermIds = phenopacketData.excludedHpoTermIds().map(Object::toString).collect(Collectors.joining(","));
            }
        }

        //TODO: add other possible separators to regex
        //TODO: only add valid termIDs to list
        List<TermId> presentHpoTermIdsList = (presentHpoTermIds == null | (presentHpoTermIds != null && presentHpoTermIds.isEmpty())) ?
                List.of() : Arrays.stream(presentHpoTermIds.split("[\\s,;]+"))
                .map(String::strip)
                .map(TermId::of)
                .toList();
        List<TermId> excludedHpoTermIdsList = (excludedHpoTermIds == null | (excludedHpoTermIds != null && excludedHpoTermIds.isEmpty())) ?
                List.of() : Arrays.stream(excludedHpoTermIds.split("[\\s,;]+"))
                .map(String::strip)
                .map(TermId::of)
                .toList();

        Sample sample = Sample.of(sampleId,
                presentHpoTermIdsList,
                excludedHpoTermIdsList);
        model.addAttribute("sample", sample);

        System.out.println("updateEngine sample = " + sample);
        System.out.println("updateEngine engine = " + engineName);

        DifferentialDiagnosisEngine engine = null;
        List<DifferentialDiagnosis> differentialDiagnoses = List.of();

        if (engineName.equals("lirical")) {
            model.addAttribute("strict", strict);
            model.addAttribute("globalAnalysisMode", globalAnalysisMode);

            AnalysisOptions options = AnalysisOptions.builder()
                    .useStrictPenalties(strict)
                    .useGlobal(globalAnalysisMode)
                    .pretestProbability(PretestDiseaseProbabilities.uniform(hpoDiseases.diseaseIds()))
                    .build();

            System.out.println(options);

            engine = liricalDifferentialDiagnosisEngineConfigurer.configure(options);
            model.addAttribute("options", options);

            if (sample != null && sample.id() != null && options != null) {
                // Get initial differential diagnoses from running LIRICAL
                differentialDiagnoses = engine.run(sample);
            }
        } else if (engineName.equals("phenomizer")) {
            if (scoringMode == null) {
                scoringMode = ScoringMode.ONE_SIDED;
            }
            model.addAttribute("scoringMode", scoringMode);

            Map<TermPair, Double> icMicaDict = icMicaData.icMicaDict();
            engine = new PhenomizerDifferentialDiagnosisEngine(hpoDiseases, icMicaDict, scoringMode);

            model.addAttribute("icMicaDict", icMicaDict);

            if (sample != null && sample.id() != null) {
                // Get initial differential diagnoses from running Phenomizer
                differentialDiagnoses = engine.run(sample);
            }
        }
        model.addAttribute("engine", engine);
        model.addAttribute("differentialDiagnoses", differentialDiagnoses);

        return "maxodiff";
    }

}
