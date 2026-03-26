package org.p2gx.maxodiff.html.controller;

import org.p2gx.maxodiff.core.SimpleTermOld;
import org.p2gx.maxodiff.core.analysis.*;
import org.p2gx.maxodiff.core.analysis.refinement.BaseDiffDiagRefiner;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.p2gx.maxodiff.core.analysis.refinement.RefinementOptions;
import org.p2gx.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;
import org.p2gx.maxodiff.core.model.PhenopacketData;
import org.p2gx.maxodiff.core.model.PpktSample;
import org.p2gx.maxodiff.core.model.RankMaxo;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.p2gx.maxodiff.html.results.tleaf.TleafResults;
import org.p2gx.maxodiff.phenomizer.IcMicaData;
import org.p2gx.maxodiff.phenomizer.PhenomizerDifferentialDiagnosisEngine;
import org.p2gx.maxodiff.phenomizer.ScoringMode;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
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

@Controller("/maxodiff")
public class MaxodiffController {

    private final IcMicaData icMicaData;

    private final BiometadataService biometadataService;

    private DiffDiagRefiner diffDiagRefiner;

    private final Ontology hpo;

    private final HpoDiseases hpoDiseases;

    private final Map<TermId, Set<TermId>> hpoToMaxoIdMap;

    private final Map<SimpleTermOld, Set<SimpleTermOld>> hpoToMaxoTermMap;

    private RankMaxo rankMaxo;

    private PpktSample sample;

    private static final Path UPLOAD_DIR = Paths.get(System.getProperty("user.home"), "maxodiff", "uploads");

    public MaxodiffController(
            IcMicaData icMicaData,
            BiometadataService biometadataService,
            DiffDiagRefiner diffDiagRefiner,
            Ontology hpo,
            HpoDiseases hpoDiseases,
            Map<TermId, Set<TermId>> hpoToMaxoIdMap,
            Map<SimpleTermOld, Set<SimpleTermOld>> hpoToMaxoTermMap
    ) {
        this.icMicaData = icMicaData;
        this.biometadataService = biometadataService;
        this.diffDiagRefiner = diffDiagRefiner;
        this.hpo = hpo;
        this.hpoDiseases = hpoDiseases;
        this.hpoToMaxoIdMap = hpoToMaxoIdMap;
        this.hpoToMaxoTermMap = hpoToMaxoTermMap;
    }

    @RequestMapping("/maxodiff")
    public String showResults(@RequestParam(value = "nDiseases", required = false) Integer nDiseases,
                              @RequestParam(value = "nRepetitions", required = false) Integer nRepetitions,
                              Model model) throws Exception {

        model.addAttribute("sample", sample);

        DifferentialDiagnosisEngine phenomizerDifferentialDxEngine = null;
        List<DifferentialDiagnosis> differentialDiagnoses = List.of();

        // Phenomizer scoring mode. Default is one-sided
        ScoringMode scoringMode = ScoringMode.ONE_SIDED;
        model.addAttribute("scoringMode", scoringMode);

        // Phenomizer icMicaData object containing term-pair similarity scores for pairs of hpo terms
        Map<TermPair, Double> icMicaDict = icMicaData.icMicaDict();
        if (icMicaDict.isEmpty()) {
            throw new Exception("Phenomizer necessary MICA information content is empty. Run Download command to download the necessary term-pair-similarity file.");
        }
        // Phenomizer differential diagnosis engine
        phenomizerDifferentialDxEngine = new PhenomizerDifferentialDiagnosisEngine(hpoDiseases, icMicaDict);
        model.addAttribute("icMicaDict", icMicaDict);

        if (sample != null && sample.id() != null) {
            // Get initial differential diagnoses from running Phenomizer
            differentialDiagnoses = phenomizerDifferentialDxEngine.run(sample);
        }

        model.addAttribute("engine", phenomizerDifferentialDxEngine);
        model.addAttribute("differentialDiagnoses", differentialDiagnoses);

        // Maxodiff refiner
        diffDiagRefiner = new BaseDiffDiagRefiner(hpoDiseases, hpoToMaxoIdMap, hpoToMaxoTermMap, hpo);

        // maxodiff analysis parameters: n diseases to use and n simulations to run
        Integer prevNDiseases = (Integer) model.getAttribute("nDiseases");
        if (nDiseases == null) {
            nDiseases = 20;
        }
        if (nRepetitions == null) {
            nRepetitions = 20;
        }
        model.addAttribute("nDiseases", nDiseases);
        model.addAttribute("nRepetitions", nRepetitions);

        if (shouldMaxoAnalysisBeRun(sample, differentialDiagnoses, nDiseases, nRepetitions)) {
            RefinementOptions options = RefinementOptions.of(nDiseases, nRepetitions);

            // n diseases subset of initial differential diagnoses in order of decreasing probability
            if (model.getAttribute("orderedDiagnoses") == null || !nDiseases.equals(prevNDiseases)) {
                List<DifferentialDiagnosis> orderedDiagnoses = diffDiagRefiner.getOrderedDiagnoses(differentialDiagnoses, options);
                model.addAttribute("orderedDiagnoses", orderedDiagnoses);
            }
            List<DifferentialDiagnosis> orderedDiagnoses = (List<DifferentialDiagnosis>) model.getAttribute("orderedDiagnoses");

            // Map of HPO Term Id and List of HpoFrequency objects for the subset n diseases.
            if (model.getAttribute("hpoTermCounts") == null || !nDiseases.equals(prevNDiseases)) {
                List<HpoDisease> diseases = diffDiagRefiner.getDiseases(orderedDiagnoses);
                Map<TermId, List<HpoFrequency>> hpoTermCounts = diffDiagRefiner.getHpoTermCounts(diseases);
                model.addAttribute("hpoTermCounts", hpoTermCounts);
            }
            Map<TermId, List<HpoFrequency>> hpoTermCounts = (Map<TermId, List<HpoFrequency>>) model.getAttribute("hpoTermCounts");

            // Map of MAxO term id : List of associated HPO term ids for the subset n diseases. HPO ancestors are removed
            if (model.getAttribute("maxoToHpoTermIdMap") == null || !nDiseases.equals(prevNDiseases)) {
                Map<TermId, Set<TermId>> maxoToHpoTermIdMap = diffDiagRefiner.getMaxoToHpoTermIdMap(hpoTermCounts);
                model.addAttribute("maxoToHpoTermIdMap", maxoToHpoTermIdMap);
            }
            Map<TermId, Set<TermId>> maxoToHpoTermIdMap = (Map<TermId, Set<TermId>>) model.getAttribute("maxoToHpoTermIdMap");

            DifferentialDiagnosisEngine diseaseSubsetEngine;
            assert orderedDiagnoses != null;
            List<DifferentialDiagnosis> initialDiagnoses = orderedDiagnoses.stream().toList()
                    .subList(0, options.nDiseases());
            Set<TermId> initialDiagnosesIds = initialDiagnoses.stream()
                    .map(DifferentialDiagnosis::diseaseId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            int totalNDiseases = differentialDiagnoses.size();
            RefinementOptions allDiseasesOptions = RefinementOptions.of(totalNDiseases, nRepetitions);
            List<DifferentialDiagnosis> allInitialDiagnoses = diffDiagRefiner.getOrderedDiagnoses(differentialDiagnoses, allDiseasesOptions);
            List<HpoDisease> allDiseases = diffDiagRefiner.getDiseases(allInitialDiagnoses);

            diseaseSubsetEngine = phenomizerDifferentialDxEngine;

            // Perform maxodiff refinement
            assert maxoToHpoTermIdMap != null;
            rankMaxo = diffDiagRefiner.getRankMaxo(allInitialDiagnoses,
                    initialDiagnoses,
                    diseaseSubsetEngine,
                    maxoToHpoTermIdMap);
            List<RankedMaxoResult> resultsList = diffDiagRefiner.runNew(sample,
                    initialDiagnosesIds,
                    options,
                    rankMaxo,
                    biometadataService);


            // Write final results to HTML
            MdMetadata mdMetadata = new MdMetadata(sample.id(),
                    nDiseases,
                    nRepetitions,
                    sample.observedHpoTerms(),
                    sample.excludedHpoTerms(),
                    resultsList);

            HTMLFrequencyMap htmlFrequencyMap = new HTMLFrequencyMap(hpoDiseases, icMicaData.icMicaDict());

            String htmlString = TleafResults.writeHTMLResults(mdMetadata, resultsList, htmlFrequencyMap);
            model.addAttribute("htmlTemplateString", htmlString);
            model.addAttribute("showMDresults", true);
        }
        return "maxodiff";
    }

    /** This is a flag indicating that the next step is to run the MAxOdiff analysis */
    private boolean shouldMaxoAnalysisBeRun(PpktSample ppktSample, List<DifferentialDiagnosis> differentialDiagnoses, Integer nDiseases, Integer nRepetitions) {
        return ppktSample != null && differentialDiagnoses != null && !differentialDiagnoses.isEmpty() && nDiseases != null && nRepetitions != null;
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
    public PpktSample updateSample(@ModelAttribute(value = "id") String sampleId,
                             @ModelAttribute(value = "observedHpoTermIds") List<TermId> observedHpoTermIds,
                             @ModelAttribute(value = "excludedHpoTermIds") List<TermId> excludedHpoTermIds,
                             Model model) {

            model.addAttribute("sampleId", sampleId);
            model.addAttribute("observedHpoTermIds", observedHpoTermIds);
            model.addAttribute("excludedHpoTermIds", excludedHpoTermIds);

            List<SimpleTerm> observedSampleTerms = new ArrayList<>();
            List<SimpleTerm> excludedSampleTerms = new ArrayList<>();
            observedHpoTermIds.forEach(tid ->
                    observedSampleTerms.add(new SimpleTerm(tid.getValue(), biometadataService.hpoLabel(tid).orElse("n/a"))));
            excludedHpoTermIds.forEach(tid ->
                    excludedSampleTerms.add(new SimpleTerm(tid.getValue(), biometadataService.hpoLabel(tid).orElse("n/a"))));
            this.sample = new PpktSample(sampleId, observedSampleTerms, excludedSampleTerms);

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
            List<TermId> observedHpoTermIds = new ArrayList<>();
            List<TermId> excludedHpoTermIds = new ArrayList<>();
            if (phenopacketPath != null) {
                PhenopacketData phenopacketData = PhenopacketData.readPhenopacketData(phenopacketPath);
                sampleId = phenopacketData.sampleId();
                observedHpoTermIds = phenopacketData.observedHpoTermIds().toList();//.map(Object::toString).collect(Collectors.joining(","));
                excludedHpoTermIds = phenopacketData.excludedHpoTermIds().toList();//.map(Object::toString).collect(Collectors.joining(","));
            }

            String phenopacketName = file.getOriginalFilename();

            result.put("phenopacketName", phenopacketName);
            result.put("id", sampleId);
            result.put("observedHpoTermIds", observedHpoTermIds);
            result.put("excludedHpoTermIds", excludedHpoTermIds);

            PpktSample ppktSample = updateSample(sampleId, observedHpoTermIds, excludedHpoTermIds, model);

            model.addAttribute("sample", ppktSample);

            Map<TermId, String> sampleObservedTermsMap = new HashMap<>();
            ppktSample.observedHpoTerms().forEach(tid ->
                    sampleObservedTermsMap.put(TermId.of(tid.termId()), tid.termLabel()));
            Map<TermId, String> sampleExcludedTermsMap = new HashMap<>();
            ppktSample.excludedHpoTerms().forEach(tid ->
                    sampleExcludedTermsMap.put(TermId.of(tid.termId()), tid.termLabel()));

            result.put("observedHpoTerms", sampleObservedTermsMap);
            result.put("excludedHpoTerms", sampleExcludedTermsMap);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
