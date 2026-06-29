package org.p2gx.maxodiff.html.controller;

import org.p2gx.maxodiff.core.analysis.*;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefinerImpl;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.p2gx.maxodiff.core.analysis.refinement.RefinementOptions;
import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.p2gx.maxodiff.core.io.JsonWriter;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;
import org.p2gx.maxodiff.core.model.PhenopacketData;
import org.p2gx.maxodiff.core.model.RankMaxo;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.p2gx.maxodiff.html.results.tleaf.TleafResults;
import org.p2gx.maxodiff.core.phenomizer.PhenomizerDDxEngine;
import org.p2gx.maxodiff.core.phenomizer.ScoringMode;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.p2gx.maxodiff.html.session.UserSessionData;
import org.phenopackets.schema.v2.Phenopacket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
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
    /* One object will be created by Spring for each Web user */
    private final UserSessionData sessionData;

    private final MdContext mdContext;

    private DiffDiagRefiner diffDiagRefiner;


    private final int nThreads;

    private static final Path UPLOAD_DIR = Paths.get(System.getProperty("user.home"), "maxodiff", "uploads");

    public MaxodiffController(
            UserSessionData sessionData,
            MdContext context,
            DiffDiagRefiner diffDiagRefiner,
            @Value("${maxodiff.threads:4}") int nthreads) {
        this.sessionData = sessionData;
        this.mdContext = context;
        this.diffDiagRefiner = diffDiagRefiner;
        this.nThreads = nthreads;
    }

    @RequestMapping("/maxodiff")
    public Object showResults(@RequestParam(value = "nDiseases", defaultValue = "20") Integer nDiseases,
                              @RequestParam(value = "nRepetitions", defaultValue = "100") Integer nRepetitions,
                              @RequestParam(value = "outputJson", required = false) boolean outputJson,
                              Model model) throws Exception {
        // reset in case user starts second analysis
        sessionData.setRankMaxo(null);
        model.addAttribute("errorMessage", null);
        model.addAttribute("hasError", false);
        PhenopacketData sample = sessionData.getPpkt();
        if (sample == null || sample.sampleId() == null) {
            model.addAttribute("showMDresults", false);
            // we will use the hasError=true to show the error on the webpage
            return "maxodiff";
        }
        model.addAttribute("sample", sample);
        Map<TermPair, Double> icMicaDict = this.mdContext.resources().icMicaData().icMicaDict();
        if (icMicaDict.isEmpty()) {
            throw new Exception("Phenomizer necessary MICA information content is empty. Run Download command to download the necessary term-pair-similarity file.");
        }
        // Phenomizer scoring mode. Default is one-sided
        ScoringMode scoringMode = ScoringMode.ONE_SIDED;
        model.addAttribute("scoringMode", scoringMode);
        DDxEngine phenomizer = new PhenomizerDDxEngine(mdContext.resources().hpoDiseases(), icMicaDict);
        List<DifferentialDiagnosis> differentialDiagnoses = phenomizer.run(sample);
       // model.addAttribute("engine", phenomizerDifferentialDxEngine);
        model.addAttribute("differentialDiagnoses", differentialDiagnoses);

        // Maxodiff refiner
        MdContext mdContextNewParams = mdContext.updateContext(nRepetitions, nDiseases);
        diffDiagRefiner = new DiffDiagRefinerImpl(mdContextNewParams);

        // maxodiff analysis parameters: n diseases to use and n simulations to run
        model.addAttribute("nDiseases", nDiseases);
        model.addAttribute("nRepetitions", nRepetitions);

        if (shouldMaxoAnalysisBeRun(sample, differentialDiagnoses, nDiseases, nRepetitions)) {
            RefinementOptions options = RefinementOptions.of(nDiseases, nRepetitions);

            // n diseases subset of initial differential diagnoses in order of decreasing probability
            if (sessionData.getOrderedDiagnoses() == null || !nDiseases.equals(sessionData.getDiagnosesCount())) {
                List<DifferentialDiagnosis> orderedDiagnoses = diffDiagRefiner.getOrderedDiagnoses(differentialDiagnoses);
                sessionData.setOrderedDiagnoses(orderedDiagnoses);
            }
            List<DifferentialDiagnosis> orderedDiagnoses = this.sessionData.getOrderedDiagnoses();

            // List of HpoFrequency objects for the subset n diseases.
            if (sessionData.getHpoTermCounts() == null || !nDiseases.equals(sessionData.getDiagnosesCount())) {
                List<HpoDisease> diseases = diffDiagRefiner.getDiseases(orderedDiagnoses);
                List<HpoFrequency> hpoTermCounts = diffDiagRefiner.getHpoFrequenciesNDiseases(diseases, mdContext.createHpoFrequencies());
                sessionData.setHpoTermCounts(hpoTermCounts);
            }
            List<HpoFrequency> hpoTermCounts = sessionData.getHpoTermCounts();

            // Map of MAxO term id : List of associated HPO term ids for the subset n diseases. HPO ancestors are removed
            if (sessionData.getMaxoToHpoTermIdMap() == null || !nDiseases.equals(sessionData.getDiagnosesCount())) {
                Map<TermId, Set<TermId>> maxoToHpoTermIdMap = diffDiagRefiner.getMaxoToHpoTermIdMap(hpoTermCounts);
                sessionData.setMaxoToHpoTermIdMap(maxoToHpoTermIdMap);
            }
            Map<TermId, Set<TermId>> maxoToHpoTermIdMap = sessionData.getMaxoToHpoTermIdMap();
            if (orderedDiagnoses == null || orderedDiagnoses.isEmpty()) {
                model.addAttribute("errorMessage", "No diagnoses retrieved. Please report to developers.");
                model.addAttribute("hasError", true);
                model.addAttribute("showMDresults", false);
                return "maxodiff";
            }
            int limit = Math.min(orderedDiagnoses.size(), options.nDiseases());
            List<DifferentialDiagnosis> initialDiagnoses = orderedDiagnoses.stream().toList()
                    .subList(0, limit);
            Set<TermId> initialDiagnosesIds = initialDiagnoses.stream()
                    .map(DifferentialDiagnosis::diseaseId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (maxoToHpoTermIdMap == null || maxoToHpoTermIdMap.isEmpty()) {
                model.addAttribute("errorMessage", "No Maxo/HPO map retrieved. Please report to developers.");
                model.addAttribute("hasError", true);
                model.addAttribute("showMDresults", false);
                return "maxodiff";
            }
            RankMaxo rankMaxo = diffDiagRefiner.getRankMaxo(
                    differentialDiagnoses,
                    orderedDiagnoses,
                    phenomizer,
                    maxoToHpoTermIdMap,
                    hpoTermCounts);
            this.sessionData.setRankMaxo(rankMaxo);
            List<RankedMaxoResult> resultsList = diffDiagRefiner.run(sample,
                    initialDiagnosesIds,
                    rankMaxo,
                    nThreads
            );
            int zeroIdx = resultsList.stream()
                    .filter(result -> result.maxoScore() == 0.)
                    .findFirst().map(resultsList::indexOf).orElse(resultsList.size());
            int nDisplayed = Math.min(resultsList.size(), zeroIdx);
            resultsList = resultsList.subList(0, nDisplayed);
            // Write final results to HTML
            MdMetadata mdMetadata = new MdMetadata(sample.sampleId(),
                    nDiseases,
                    nRepetitions,
                    sample.observed(),
                    sample.excluded(),
                    resultsList);

            model.addAttribute("outputJson", outputJson);
            if (outputJson) {
               return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(mdMetadata);
            } else {
                HTMLFrequencyMap htmlFrequencyMap = new HTMLFrequencyMap(mdContext);

                String htmlString = TleafResults.writeHTMLResults(mdMetadata, resultsList, htmlFrequencyMap);
                model.addAttribute("htmlTemplateString", htmlString);
                model.addAttribute("showMDresults", true);
            }

        }
        // Spring finds maxodiff.html, injects the data, and sends page to the user's browser.
        return "maxodiff";
    }

    /**
     * This is a flag indicating that the next step is to run the MAxOdiff analysis
     */
    private boolean shouldMaxoAnalysisBeRun(PhenopacketData ppktSample, List<DifferentialDiagnosis> differentialDiagnoses, Integer nDiseases, Integer nRepetitions) {
        return ppktSample != null && differentialDiagnoses != null && !differentialDiagnoses.isEmpty() && nDiseases != null && nRepetitions != null;
    }

    @GetMapping("progress1")
    @ResponseBody
    public double getProgress() {
        RankMaxo rankMaxo = this.sessionData.getRankMaxo();
        return rankMaxo == null ? 0.01 : rankMaxo.getRankMaxoProgress().getTotalProgress();
    }

    /**
     * Returns a double. This is used by a "Progress Bar" on your page.
     * While the heavy analysis is running, a small piece of JavaScript
     * on your webpage asks /progress1 every second about the progress.
     *
     * @return
     */
    @GetMapping("progress-bar1")
    public String showProgressPage() {
        return "progress1";
    }


    public PhenopacketData updateSample(String sampleId,
                                        List<TermId> observedHpoTermIds,
                                        List<TermId> excludedHpoTermIds,
                                        Model model) {

        model.addAttribute("sampleId", sampleId);
        model.addAttribute("observedHpoTermIds", observedHpoTermIds);
        model.addAttribute("excludedHpoTermIds", excludedHpoTermIds);

        List<SimpleTerm> observedSampleTerms = new ArrayList<>();
        List<SimpleTerm> excludedSampleTerms = new ArrayList<>();
        BiometadataService biometadataService = this.mdContext.biometadataService();
        observedHpoTermIds.forEach(tid ->
                observedSampleTerms.add(new SimpleTerm(tid, biometadataService.hpoLabel(tid).orElse("n/a"))));
        excludedHpoTermIds.forEach(tid ->
                excludedSampleTerms.add(new SimpleTerm(tid, biometadataService.hpoLabel(tid).orElse("n/a"))));
        return new PhenopacketData(sampleId, observedSampleTerms, excludedSampleTerms, List.of(), List.of(), false);
    }

    /**
     * The front receives this JSON, sees that the upload was successful,
     * and then manually tells the browser to go to the /maxodiff page.
     *
     * @param file  The phenopacket JSON file to be uploaded
     * @param model
     * @return a ResponseEntity<Map>. This is JSON data.
     */
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

            PhenopacketData ppktSample = updateSample(sampleId, observedHpoTermIds, excludedHpoTermIds, model);
            this.sessionData.setPpkt(ppktSample);
            model.addAttribute("sample", ppktSample);

            Map<TermId, String> sampleObservedTermsMap = new HashMap<>();
            ppktSample.observed().forEach(st ->
                    sampleObservedTermsMap.put(st.tid(), st.label()));
            Map<TermId, String> sampleExcludedTermsMap = new HashMap<>();
            ppktSample.excluded().forEach(st ->
                    sampleExcludedTermsMap.put(st.tid(), st.label()));

            result.put("observedHpoTerms", sampleObservedTermsMap);
            result.put("excludedHpoTerms", sampleExcludedTermsMap);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping(
        value = "/api/analyze", 
        consumes = MediaType.APPLICATION_JSON_VALUE, 
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<MdMetadata> analyzeJson(@RequestBody Phenopacket payload) throws Exception {

        if (payload == null || payload.getId() == null) {
            return ResponseEntity.badRequest().build();
        }

        PhenopacketData ppktData = PhenopacketData.fromPpkt(payload);

        int nDiseases = 20;
        int nRepetitions = 80;

        // 1. Initial Differential Diagnosis Engine (Phenomizer)
        Map<TermPair, Double> icMicaDict = this.mdContext.resources().icMicaData().icMicaDict();
        if (icMicaDict.isEmpty()) {
            throw new IllegalStateException("Phenomizer resource MICA information content is empty.");
        }
        
        DDxEngine phenomizer = new PhenomizerDDxEngine(mdContext.resources().hpoDiseases(), icMicaDict);
        List<DifferentialDiagnosis> differentialDiagnoses = phenomizer.run(ppktData);

        // 2. Maxodiff Refiner Engine (Using updated parameters from RequestParams)
        MdContext mdContextNewParams = mdContext.updateContext(nRepetitions, nDiseases);
        DiffDiagRefiner customRefiner = new DiffDiagRefinerImpl(mdContextNewParams);

        // 3. Process RankMaxo and Refinement Math Pipelines
        List<DifferentialDiagnosis> orderedDiagnoses = customRefiner.getOrderedDiagnoses(differentialDiagnoses);
        List<HpoDisease> diseases = customRefiner.getDiseases(orderedDiagnoses);
        List<HpoFrequency> hpoTermCounts = customRefiner.getHpoFrequenciesNDiseases(diseases, mdContext.createHpoFrequencies());
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = customRefiner.getMaxoToHpoTermIdMap(hpoTermCounts);

        int limit = Math.min(orderedDiagnoses.size(), nDiseases);
        List<DifferentialDiagnosis> initialDiagnoses = orderedDiagnoses.subList(0, limit);
        Set<TermId> initialDiagnosesIds = initialDiagnoses.stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        RankMaxo rankMaxo = customRefiner.getRankMaxo(
                differentialDiagnoses,
                orderedDiagnoses,
                phenomizer,
                maxoToHpoTermIdMap,
                hpoTermCounts);

        List<RankedMaxoResult> resultsList = customRefiner.run(ppktData, initialDiagnosesIds, rankMaxo, nThreads);

        // 4. Wrap up the core computation metadata object and return it as JSON
        MdMetadata mdMetadata = new MdMetadata(
                ppktData.sampleId(),
                nDiseases,
                nRepetitions,
                ppktData.observed(),
                ppktData.excluded(),
                resultsList);

        return ResponseEntity.ok(mdMetadata);
    }

}
