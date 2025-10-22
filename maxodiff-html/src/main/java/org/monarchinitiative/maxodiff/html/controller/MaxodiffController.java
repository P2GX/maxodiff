package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.refinement.*;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.PhenopacketData;
import org.monarchinitiative.maxodiff.core.model.RankMaxo;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.html.results.HtmlResults;
import org.monarchinitiative.maxodiff.phenomizer.IcMicaData;
import org.monarchinitiative.maxodiff.phenomizer.PhenomizerDifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.phenomizer.ScoringMode;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller("/maxodiff")
public class MaxodiffController {

    private final IcMicaData icMicaData;

    private final BiometadataService biometadataService;

    private DiffDiagRefiner diffDiagRefiner;

    private final MinimalOntology minHpo;

    private final Ontology hpo;

    private final HpoDiseases hpoDiseases;

    private final Map<TermId, Set<TermId>> hpoToMaxoIdMap;

    private final Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap;

    private RankMaxo rankMaxo;

    private static final Path UPLOAD_DIR = Paths.get(System.getProperty("user.home"), "maxodiff", "uploads");

    public MaxodiffController(
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
        this.biometadataService = biometadataService;
        this.diffDiagRefiner = diffDiagRefiner;
        this.minHpo = minHpo;
        this.hpo = hpo;
        this.hpoDiseases = hpoDiseases;
        this.hpoToMaxoIdMap = hpoToMaxoIdMap;
        this.hpoToMaxoTermMap = hpoToMaxoTermMap;
    }

    @RequestMapping("/maxodiff")
    public String showResults(@RequestParam(value = "id", required = false) String sampleId,
                              @RequestParam(value = "observedHpoTermIds", required = false) String observedHpoTermIds,
                              @RequestParam(value = "excludedHpoTermIds", required = false) String excludedHpoTermIds,
                              @RequestParam(value = "nDiseases", required = false) Integer nDiseases,
                              @RequestParam(value = "nRepetitions", required = false) Integer nRepetitions,
                              @RequestParam(value = "view", required = false) String view,
                              Model model) throws Exception {

        model.addAttribute("sampleId", sampleId);
        model.addAttribute("observedHpoTermIds", observedHpoTermIds);
        model.addAttribute("excludedHpoTermIds", excludedHpoTermIds);
        model.addAttribute("view", view);

        List<TermId> observedHpoTermIdsList = (observedHpoTermIds == null | (observedHpoTermIds != null && observedHpoTermIds.isEmpty())) ?
                List.of() : Arrays.stream(observedHpoTermIds.split("[\\s,;]+"))
                .map(String::strip)
                .map(TermId::of)
                .toList();
        List<TermId> excludedHpoTermIdsList = (excludedHpoTermIds == null | (excludedHpoTermIds != null && excludedHpoTermIds.isEmpty())) ?
                List.of() : Arrays.stream(excludedHpoTermIds.split("[\\s,;]+"))
                .map(String::strip)
                .map(TermId::of)
                .toList();

        Sample sample = Sample.of(sampleId,
                observedHpoTermIdsList,
                excludedHpoTermIdsList);
        model.addAttribute("sample", sample);

        DifferentialDiagnosisEngine phenomizerDifferentialDxEngine = null;
        List<DifferentialDiagnosis> differentialDiagnoses = List.of();


        ScoringMode scoringMode = ScoringMode.ONE_SIDED;
        model.addAttribute("scoringMode", scoringMode);

        Map<TermPair, Double> icMicaDict = icMicaData.icMicaDict();
        if (icMicaDict.isEmpty()) {
            throw new Exception("Phenomizer necessary MICA information content is empty. Run Download command to download the necessary term-pair-similarity file.");
        }
        phenomizerDifferentialDxEngine = new PhenomizerDifferentialDiagnosisEngine(hpoDiseases, icMicaDict, scoringMode);
        model.addAttribute("icMicaDict", icMicaDict);

        if (sample != null && sample.id() != null) {
            // Get initial differential diagnoses from running Phenomizer
            differentialDiagnoses = phenomizerDifferentialDxEngine.run(sample);
        }

        model.addAttribute("engine", phenomizerDifferentialDxEngine);
        model.addAttribute("differentialDiagnoses", differentialDiagnoses);

        diffDiagRefiner = new MaxoDiffRefiner(hpoDiseases, hpoToMaxoIdMap, hpoToMaxoTermMap, minHpo, hpo);

        Integer prevNDiseases = (Integer) model.getAttribute("nDiseases");
        model.addAttribute("nDiseases", nDiseases);
        model.addAttribute("nRepetitions", nRepetitions);

        if (shouldMaxoAnalysisBeRun(sample, differentialDiagnoses, nDiseases, nRepetitions)) {
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
                List<TermId> termIdsToRemove = List.of();
                Map<TermId, Set<TermId>> maxoToHpoTermIdMap = diffDiagRefiner.getMaxoToHpoTermIdMap(termIdsToRemove, hpoTermCounts);
                model.addAttribute("maxoToHpoTermIdMap", maxoToHpoTermIdMap);
            }
            Map<TermId, Set<TermId>> maxoToHpoTermIdMap = (Map<TermId, Set<TermId>>) model.getAttribute("maxoToHpoTermIdMap");

            RefinementResults refinementResults;
            DifferentialDiagnosisEngine diseaseSubsetEngine;
            assert orderedDiagnoses != null;
            List<DifferentialDiagnosis> initialDiagnoses = orderedDiagnoses.stream().toList()
                    .subList(0, options.nDiseases());
            int totalNDiseases = differentialDiagnoses.size();
            RefinementOptions allDiseasesOptions = RefinementOptions.of(totalNDiseases, nRepetitions);
            List<DifferentialDiagnosis> allInitialDiagnoses = diffDiagRefiner.getOrderedDiagnoses(differentialDiagnoses, allDiseasesOptions);
            List<HpoDisease> allDiseases = diffDiagRefiner.getDiseases(allInitialDiagnoses);
            Map<TermId, List<HpoFrequency>> allHpoTermCounts = diffDiagRefiner.getHpoTermCounts(allDiseases);

            diseaseSubsetEngine = phenomizerDifferentialDxEngine;

            assert maxoToHpoTermIdMap != null;
            String diseaseProbModel = "ranked";
            rankMaxo = ((MaxoDiffRefiner) diffDiagRefiner).getRankMaxo(allInitialDiagnoses,
                    initialDiagnoses,
                    diseaseSubsetEngine,
                    maxoToHpoTermIdMap,
                    diseaseProbModel);
            refinementResults = diffDiagRefiner.run(sample,
                    orderedDiagnoses,
                    options,
                    rankMaxo,
                    allHpoTermCounts,
                    maxoToHpoTermIdMap);


            List<MaxodiffResult> resultsList = new ArrayList<>(refinementResults.maxodiffResults());
            resultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.rankMaxoScore().maxoScore()).reversed());


            String htmlString = HtmlResults.writeHTMLResults(
                    sample,
                    nDiseases,
                    hpoDiseases,
                    nRepetitions,
                    resultsList,
                    biometadataService,
                    hpoTermCounts,
                    icMicaDict,
                    view);
            model.addAttribute("htmlTemplateString", htmlString);
            model.addAttribute("showMDresults", true);
        }
        return "maxodiff";
    }

    /** This is a flag indicating that the next step is to run the MAxOdiff analysis */
    private boolean shouldMaxoAnalysisBeRun(Sample sample, List<DifferentialDiagnosis> differentialDiagnoses, Integer nDiseases, Integer nRepetitions) {
        return sample != null && differentialDiagnoses != null && nDiseases != null && nRepetitions != null;
    }

    @GetMapping("progress1")
    @ResponseBody
    public double getProgress() {
        return rankMaxo == null ? 0.01 : rankMaxo.getRankMaxoProgress().getTotalProgress();
    }

    @GetMapping("progress-bar1")
    public String showProgressPage() {
        return "progress1";
    }


//    @GetMapping("/updateSample")
    public Sample updateSample(@RequestParam(value = "id", required = false) String sampleId,
                             @RequestParam(value = "observedHpoTermIds", required = false) String observedHpoTermIds,
                             @RequestParam(value = "excludedHpoTermIds", required = false) String excludedHpoTermIds,
                             Model model) {

            model.addAttribute("sampleId", sampleId);
            model.addAttribute("observedHpoTermIds", observedHpoTermIds);
            model.addAttribute("excludedHpoTermIds", excludedHpoTermIds);

            List<TermId> observedHpoTermIdsList = (observedHpoTermIds == null | (observedHpoTermIds != null && observedHpoTermIds.isEmpty())) ?
                    List.of() : Arrays.stream(observedHpoTermIds.split("[\\s,;]+"))
                    .map(String::strip)
                    .map(TermId::of)
                    .toList();
            List<TermId> excludedHpoTermIdsList = (excludedHpoTermIds == null | (excludedHpoTermIds != null && excludedHpoTermIds.isEmpty())) ?
                    List.of() : Arrays.stream(excludedHpoTermIds.split("[\\s,;]+"))
                    .map(String::strip)
                    .map(TermId::of)
                    .toList();

            Sample sample = Sample.of(sampleId,
                    observedHpoTermIdsList,
                    excludedHpoTermIdsList);
            model.addAttribute("sample", sample);

//            System.out.println("updateSample sample = " + sample);

            return sample;
    }

    @RequestMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file, Model model) {

        Map<String, Object> result = new HashMap<>();
        try {

            if (!Files.exists(UPLOAD_DIR)) {
                Files.createDirectories(UPLOAD_DIR);
            }

            Path phenopacketPath = UPLOAD_DIR.resolve(Objects.requireNonNull(file.getOriginalFilename()));
            file.transferTo(phenopacketPath.toFile());

            String sampleId = "";
            String observedHpoTermIds = "";
            String excludedHpoTermIds = "";
            if (phenopacketPath != null) {
                PhenopacketData phenopacketData = PhenopacketData.readPhenopacketData(phenopacketPath);
                sampleId = phenopacketData.sampleId();
                observedHpoTermIds = phenopacketData.observedHpoTermIds().map(Object::toString).collect(Collectors.joining(","));
                excludedHpoTermIds = phenopacketData.excludedHpoTermIds().map(Object::toString).collect(Collectors.joining(","));
            }

            String phenopacketName = file.getOriginalFilename();

            result.put("phenopacketName", phenopacketName);
            result.put("id", sampleId);
            result.put("observedHpoTermIds", observedHpoTermIds);
            result.put("excludedHpoTermIds", excludedHpoTermIds);

            Sample sample = updateSample(sampleId, observedHpoTermIds, excludedHpoTermIds, model);

            model.addAttribute("sample", sample);

            Map<TermId, String> sampleObservedTermsMap = new HashMap<>();
            sample.observedHpoTermIds().forEach(tid ->
                    sampleObservedTermsMap.put(tid, biometadataService.hpoLabel(tid).orElse("unknown")));
            Map<TermId, String> sampleExcludedTermsMap = new HashMap<>();
            sample.excludedHpoTermIds().forEach(tid ->
                    sampleExcludedTermsMap.put(tid, biometadataService.hpoLabel(tid).orElse("unknown")));

            result.put("observedHpoTerms", sampleObservedTermsMap);
            result.put("excludedHpoTerms", sampleExcludedTermsMap);

            model.addAttribute("observedHpoTerms", sampleObservedTermsMap);
            model.addAttribute("excludedHpoTerms", sampleExcludedTermsMap);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
