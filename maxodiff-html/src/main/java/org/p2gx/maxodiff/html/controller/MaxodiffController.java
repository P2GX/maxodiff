package org.p2gx.maxodiff.html.controller;

import org.p2gx.maxodiff.core.MaxodiffAnalysisRunner;
import org.p2gx.maxodiff.core.analysis.*;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefinerImpl;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.p2gx.maxodiff.core.analysis.refinement.RefinementOptions;
import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;
import org.p2gx.maxodiff.core.model.PhenopacketData;
import org.p2gx.maxodiff.core.model.RankMaxo;
import org.p2gx.maxodiff.core.model.TermPair;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.p2gx.maxodiff.html.config.AnalysisGate;
import org.p2gx.maxodiff.html.config.ServerBusyException;
import org.p2gx.maxodiff.html.results.tleaf.TleafResults;
import org.p2gx.maxodiff.core.phenomizer.PhenomizerDDxEngine;
import org.p2gx.maxodiff.core.phenomizer.ScoringMode;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.html.session.UserSessionData;
import org.phenopackets.schema.v2.Phenopacket;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Controller("/maxodiff")
public class MaxodiffController {
    /* One object will be created by Spring for each Web user */
    private final UserSessionData sessionData;

    private final MdContext mdContext;

    private final ExecutorService executor;

    private final AnalysisGate analysisGate;

    public MaxodiffController(
            UserSessionData sessionData,
            MdContext context,
            ExecutorService executor,
            AnalysisGate analysisGate) {
        this.sessionData = sessionData;
        this.mdContext = context;
        this.executor = executor;
        this.analysisGate = analysisGate;
    }

    private ModelAndView problemPage(Model model, HttpStatus status, List<UserFacingProblem> problems) {
        model.addAttribute("problems", problems);
        model.addAttribute("showMDresults", false);
        PhenopacketData sample = sessionData.getPpkt();
        if (sample != null) {
            model.addAttribute("sample", sample);   // keeps the Run button enabled
        }
        return new ModelAndView("maxodiff", model.asMap(), status);   // same page, form still visible
    }


    @RequestMapping("/maxodiff")
    public Object showResults(@RequestParam(value = "nDiseases", defaultValue = "20") Integer nDiseases,
                              @RequestParam(value = "nRepetitions", defaultValue = "100") Integer nRepetitions,
                              @RequestParam(value = "outputJson", required = false) boolean outputJson,
                              Model model) throws Exception {
        List<UserFacingProblem> problems = AnalysisLimits.check(nDiseases, nRepetitions);
        if (!problems.isEmpty()) {
            return problemPage(model, HttpStatus.BAD_REQUEST, problems);
        }
        // reset in case user starts second analysis
        sessionData.setRankMaxo(null);
        PhenopacketData sample = sessionData.getPpkt();
        if (sample == null || sample.sampleId() == null) {
            // Expected on the first visit to the app: no phenopacket has been uploaded yet, so we just show the
            // empty form (the Run button stays disabled until a file is uploaded).
            model.addAttribute("showMDresults", false);
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
        DiffDiagRefiner refinar = new DiffDiagRefinerImpl(mdContextNewParams);

        // maxodiff analysis parameters: n diseases to use and n simulations to run
        model.addAttribute("nDiseases", nDiseases);
        model.addAttribute("nRepetitions", nRepetitions);

        if (shouldMaxoAnalysisBeRun(sample, differentialDiagnoses, nDiseases, nRepetitions)) {
            RefinementOptions options = RefinementOptions.of(nDiseases, nRepetitions);

            // n diseases subset of initial differential diagnoses in order of decreasing probability
            if (sessionData.getOrderedDiagnoses() == null || !nDiseases.equals(sessionData.getDiagnosesCount())) {
                List<DifferentialDiagnosis> orderedDiagnoses = refinar.getOrderedDiagnoses(differentialDiagnoses);
                sessionData.setOrderedDiagnoses(orderedDiagnoses);
            }
            List<DifferentialDiagnosis> orderedDiagnoses = this.sessionData.getOrderedDiagnoses();

            // List of HpoFrequency objects for the subset n diseases.
            if (sessionData.getHpoTermCounts() == null || !nDiseases.equals(sessionData.getDiagnosesCount())) {
                List<HpoDisease> diseases = refinar.getDiseases(orderedDiagnoses);
                List<HpoFrequency> hpoTermCounts = refinar.getHpoFrequenciesNDiseases(diseases, mdContext.createHpoFrequencies());
                sessionData.setHpoTermCounts(hpoTermCounts);
            }
            List<HpoFrequency> hpoTermCounts = sessionData.getHpoTermCounts();

            // Map of MAxO term id : List of associated HPO term ids for the subset n diseases. HPO ancestors are removed
            if (sessionData.getMaxoToHpoTermIdMap() == null || !nDiseases.equals(sessionData.getDiagnosesCount())) {
                Map<TermId, Set<TermId>> maxoToHpoTermIdMap = refinar.getMaxoToHpoTermIdMap(hpoTermCounts);
                sessionData.setMaxoToHpoTermIdMap(maxoToHpoTermIdMap);
            }
            Map<TermId, Set<TermId>> maxoToHpoTermIdMap = sessionData.getMaxoToHpoTermIdMap();
            if (orderedDiagnoses == null || orderedDiagnoses.isEmpty()) {
                return problemPage(model, HttpStatus.INTERNAL_SERVER_ERROR, List.of(new UserFacingProblem(
                        "No candidate diagnoses could be computed",
                        "The service could not derive any candidate diagnoses from this patient file.",
                        "Please check that the file lists observed phenotypes (HPO terms). If it does, please contact the developer team.")));
            }
            int limit = Math.min(orderedDiagnoses.size(), options.nDiseases());
            List<DifferentialDiagnosis> initialDiagnoses = orderedDiagnoses.stream().toList()
                    .subList(0, limit);
            Set<TermId> initialDiagnosesIds = initialDiagnoses.stream()
                    .map(DifferentialDiagnosis::diseaseId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (maxoToHpoTermIdMap == null || maxoToHpoTermIdMap.isEmpty()) {
                return problemPage(model, HttpStatus.INTERNAL_SERVER_ERROR, List.of(new UserFacingProblem(
                        "Reference data is not available",
                        "The data that links medical tests to phenotypes could not be loaded on this server.",
                        "This cannot be fixed from your side. Please contact the developer team.")));
            }

            // Maxodiff Refiner Engine (Using updated parameters from RequestParams)
            DiffDiagRefiner customRefiner = new DiffDiagRefinerImpl(mdContextNewParams);

            DDxEngine engine = new PhenomizerDDxEngine(mdContext);
            MaxodiffAnalysisRunner runner = new MaxodiffAnalysisRunner(
                    mdContext,
                    executor,
                    engine,
                    customRefiner,
                    hpoTermCounts);
            List<RankedMaxoResult> resultsList;
            try (AnalysisGate.Ticket ignored = analysisGate.enter()) {
                resultsList = runner.analyzeSample(sample);
            } catch (ServerBusyException busy) {
                return problemPage(model, HttpStatus.SERVICE_UNAVAILABLE, List.of(new UserFacingProblem(
                        "The server is busy",
                        "Other analyses are using all of the available capacity, and none became free in time.",
                        "Please wait a few minutes and run the analysis again.")));
            }

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

            Path phenopacketPath = Files.createTempFile("", file.getOriginalFilename());
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
    public ResponseEntity<?> analyzeJson(
            @RequestBody Phenopacket payload,
            @RequestParam(value = "nDiseases", defaultValue = "20") Integer nDiseases,
            @RequestParam(value = "nRepetitions", defaultValue = "80") Integer nRepetitions) throws Exception {
        List<UserFacingProblem> problems = new ArrayList<>(AnalysisLimits.checkPhenopacket(payload));
        problems.addAll(AnalysisLimits.check(nDiseases, nRepetitions));
        if (!problems.isEmpty()) {
            return badRequest(problems);
        }
        try (AnalysisGate.Ticket ignored = analysisGate.enter()) {
            return ResponseEntity.ok(analyze(payload, nDiseases, nRepetitions));
        }
    }

    private static ResponseEntity<ApiProblems> badRequest(List<UserFacingProblem> problems) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ApiProblems(problems));
    }

    @ExceptionHandler(ServerBusyException.class)
    public ResponseEntity<ApiProblems> serverBusy() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.RETRY_AFTER, "60")
                .body(new ApiProblems(List.of(new UserFacingProblem(
                        "The server is busy",
                        "Other analyses are using all of the available capacity, and none became free in time.",
                        "Please wait a few minutes and send the request again."))));
    }

    private MdMetadata analyze(Phenopacket payload, int nDiseases, int nRepetitions) throws Exception {
        PhenopacketData ppktData = PhenopacketData.fromPpkt(payload);


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

        DDxEngine engine = new PhenomizerDDxEngine(mdContext);
        List<RankedMaxoResult> resultsList;
        MaxodiffAnalysisRunner runner = new MaxodiffAnalysisRunner(
                mdContext,
                executor,
                engine,
                customRefiner,
                hpoTermCounts);
        resultsList = runner.analyzeSample(ppktData);

        return new MdMetadata(
                ppktData.sampleId(),
                nDiseases,
                nRepetitions,
                ppktData.observed(),
                ppktData.excluded(),
                resultsList);
    }

    @PostMapping(
        value = "/api/modality", 
        consumes = MediaType.APPLICATION_JSON_VALUE, 
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> analyzeBestModalityJson(
            @RequestBody Phenopacket payload,
            @RequestParam(value = "targetDiseaseId", required = true) String targetDiseaseId,
            @RequestParam(value = "nDiseases", defaultValue = "20") Integer nDiseases,
            @RequestParam(value = "nRepetitions", defaultValue = "80") Integer nRepetitions) throws Exception {
        List<UserFacingProblem> problems = new ArrayList<>(AnalysisLimits.checkPhenopacket(payload));
        problems.addAll(AnalysisLimits.checkTargetDisease(targetDiseaseId));
        problems.addAll(AnalysisLimits.check(nDiseases, nRepetitions));
        if (!problems.isEmpty()) {
            return badRequest(problems);
        }
        try (AnalysisGate.Ticket ignored = analysisGate.enter()) {
            return ResponseEntity.ok(analyzeBestModality(payload, targetDiseaseId, nDiseases, nRepetitions));
        }
    }

    private List<RankedMaxoResultSingleDisease> analyzeBestModality(
            Phenopacket payload,
            String targetDiseaseId,
            int nDiseases,
        int nRepetitions)
            throws Exception {
        PhenopacketData ppktData = PhenopacketData.fromPpkt(payload);



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
        TermId targetId = TermId.of(targetDiseaseId); 
        
        // 3. Process RankMaxo and Refinement Math Pipelines
        List<DifferentialDiagnosis> orderedDiagnoses = customRefiner.getOrderedDiagnoses(differentialDiagnoses);
        List<HpoDisease> diseases = customRefiner.getDiseases(orderedDiagnoses);
        List<HpoFrequency> hpoTermCounts = customRefiner.getHpoFrequenciesNDiseases(diseases, mdContext.createHpoFrequencies());

        DDxEngine engine = new PhenomizerDDxEngine(mdContext);
        List<RankedMaxoResultSingleDisease> resultsList;
        MaxodiffAnalysisRunner runner = new MaxodiffAnalysisRunner(
                mdContext,
                executor,
                engine,
                customRefiner,
                hpoTermCounts);
        resultsList = runner.analyzeSampleSingleDisease(ppktData, targetId);

        // Make a Record that has enough data to show HTML page on SAMS, e.g., also show name of phenopacket, and some other things (check with DS)
        return resultsList.stream()
                .sorted(Comparator.comparingDouble(RankedMaxoResultSingleDisease::maxoScore).reversed())
                .toList();
    }

}
