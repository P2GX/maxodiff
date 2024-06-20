package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.html.service.LiricalInputService;
import org.monarchinitiative.maxodiff.lirical.LiricalConfiguration;
import org.monarchinitiative.maxodiff.lirical.LiricalDifferentialDiagnosisEngineConfigurer;
import org.monarchinitiative.maxodiff.lirical.LiricalRecord;
import org.monarchinitiative.maxodiff.core.io.PhenopacketFileParser;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.html.analysis.InputRecord;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;


@Controller
@RequestMapping("/liricalInput")
@SessionAttributes({"liricalRecord", "inputRecord"})
public class LiricalInputController {

    private final Path liricalDir;
    private final LiricalRecord defaultLiricalRecord;


    public LiricalInputController(Path liricalDataDir,
                                  LiricalRecord defaultLiricalRecord) {

        this.liricalDir = liricalDataDir;
        this.defaultLiricalRecord = defaultLiricalRecord;
    }


    @RequestMapping
    public String liricalInput(
//            @SessionAttribute("sample") Sample sample,
            @RequestParam(value = "genomeBuild", required = false) String genomeBuild,
            @RequestParam(value = "transcriptDatabase", required = false) TranscriptDatabase transcriptDatabase,
            @RequestParam(value = "pathogenicityThreshold", required = false) Float pathogenicityThreshold,
            @RequestParam(value = "defaultVariantBackgroundFrequency", required = false) Double defaultVariantBackgroundFrequency,
            @RequestParam(value = "strict", required = false) boolean strict,
            @RequestParam(value = "globalAnalysisMode", required = false) boolean globalAnalysisMode,
            @RequestParam(value = "phenopacketPath", required = false) Path phenopacketPath,
            Model model) throws Exception {

        //Run LIRICAL calculation and add records to model
        //TODO: remove record and assign variables separately
        LiricalRecord liricalRecord = new LiricalRecord(genomeBuild, transcriptDatabase, pathogenicityThreshold, defaultVariantBackgroundFrequency,
                                                        strict, globalAnalysisMode, liricalDir, null, null);
        if (genomeBuild == null) {
            liricalRecord = defaultLiricalRecord;
        }
        

        model.addAttribute("liricalRecord", liricalRecord);
        LiricalConfiguration liricalConfiguration = LiricalInputService.liricalConfiguration(liricalRecord);
        LiricalDifferentialDiagnosisEngineConfigurer configurer = LiricalInputService.configureLiricalConfigurer(liricalConfiguration.lirical().analysisRunner());
        //TODO: ideally start with the engine options, remove configuration from lines 54-63
        AnalysisOptions options = liricalConfiguration.prepareAnalysisOptions();
        System.out.println(options);
        //TODO: make separate page for DifferentialDiagnosisEngineService to implement engine as SessionAttribute
        DifferentialDiagnosisEngine engine = configurer.configure(options);

        InputRecord inputRecord = new InputRecord(null, null);
        model.addAttribute("phenopacketPath", phenopacketPath);
        if (phenopacketPath != null) {
            System.out.println(phenopacketPath);
            PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(phenopacketPath);
            Sample sample = Sample.of(phenopacketData.sampleId(),
                    phenopacketData.presentHpoTermIds().toList(),
                    phenopacketData.excludedHpoTermIds().toList());

            // Get initial differential diagnoses from running LIRICAL
            List<DifferentialDiagnosis> differentialDiagnoses = engine.run(sample);
            inputRecord = new InputRecord(sample, differentialDiagnoses);
        }

//        if (sample != null) {
//            // Get initial differential diagnoses from running LIRICAL
//            List<DifferentialDiagnosis> differentialDiagnoses = engine.run(sample);
//            inputRecord = new InputRecord(sample, differentialDiagnoses);
//        }
        model.addAttribute("inputRecord", inputRecord);

        return "liricalInput";
    }

}
