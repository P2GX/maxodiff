package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.LiricalAnalysisRunner;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.maxodiff.config.PropertiesLoader;
import org.monarchinitiative.maxodiff.html.service.InputService;
import org.monarchinitiative.maxodiff.lirical.LiricalRecord;
import org.monarchinitiative.maxodiff.core.io.PhenopacketFileParser;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.html.analysis.InputRecord;
import org.monarchinitiative.maxodiff.html.service.SessionResultsService;
import org.monarchinitiative.maxodiff.lirical.LiricalDifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.lirical.LiricalConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;


@Controller
@RequestMapping("/input")
@SessionAttributes({"liricalRecord", "inputRecord"})
public class InputController {


    @Autowired
    SessionResultsService sessionResultsService = new SessionResultsService();


    @RequestMapping
    public String input(
            // TODO: LIRICAL data dir should not be here.
//            @RequestParam(value = "liricalDataDir", required = false) Path liricalDataDir,
            @RequestParam(value = "genomeBuild", required = false) String genomeBuild,
            @RequestParam(value = "transcriptDatabase", required = false) TranscriptDatabase transcriptDatabase,
            @RequestParam(value = "pathogenicityThreshold", required = false) Float pathogenicityThreshold,
            @RequestParam(value = "defaultVariantBackgroundFrequency", required = false) Double defaultVariantBackgroundFrequency,
            @RequestParam(value = "strict", required = false) boolean strict,
            @RequestParam(value = "globalAnalysisMode", required = false) boolean globalAnalysisMode,
            // TODO: Exomiser path should not be defined here.
            @RequestParam(value = "exomiserPath", required = false) Path exomiserPath,
            @RequestParam(value = "vcfPath", required = false) Path vcfPath,
//            @RequestParam(value = "maxodiffDir", required = false) Path maxodiffDir,
            @RequestParam(value = "phenopacketPath", required = false) Path phenopacketPath,
            Model model) throws Exception {

        //TODO: load elsewhere to be used in all HTML files
        String propFile = PropertiesLoader.getPropertiesFilepath("application.properties");
        Properties props = PropertiesLoader.loadProperties(propFile);
        String liricalPropFile = PropertiesLoader.getPropertiesFilepath("maxodiff.lirical.properties");
        Properties liricalProps = PropertiesLoader.loadProperties(liricalPropFile);

        InputService inputService = InputService.of(props, liricalProps);

        //Run LIRICAL calculation and add records to model
        // TODO: at this place, we need to use the "analysis" service
        // instead of hard-coded LIRICAL
        Path liricalDataDir = Path.of(props.getProperty("lirical-data-directory"));
        LiricalRecord liricalRecord = new LiricalRecord(genomeBuild, transcriptDatabase, pathogenicityThreshold, defaultVariantBackgroundFrequency,
                                                        strict, globalAnalysisMode, liricalDataDir, exomiserPath, vcfPath);
        if (genomeBuild == null) {
            liricalRecord = inputService.getDefaultLiricalRecord();
        }
        model.addAttribute("liricalRecord", liricalRecord);
        LiricalConfiguration liricalConfiguration = LiricalConfiguration.of(liricalRecord);
        LiricalAnalysisRunner liricalAnalysisRunner = liricalConfiguration.lirical().analysisRunner();
        AnalysisOptions options = liricalConfiguration.prepareAnalysisOptions();

        Path maxodiffDir = Path.of(props.getProperty("maxodiff-data-directory"));
        InputRecord inputRecord = new InputRecord(null, null, maxodiffDir, phenopacketPath);
        if (phenopacketPath != null) {
            System.out.println(phenopacketPath);
            PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(phenopacketPath);
            Sample sample = Sample.of(phenopacketData.sampleId(),
                    phenopacketData.presentHpoTermIds().toList(),
                    phenopacketData.excludedHpoTermIds().toList());

            // Get initial differential diagnoses from running LIRICAL
            LiricalDifferentialDiagnosisEngine engine = new LiricalDifferentialDiagnosisEngine(liricalAnalysisRunner, options);
            List<DifferentialDiagnosis> differentialDiagnoses = engine.run(sample);
            inputRecord = new InputRecord(sample, differentialDiagnoses, maxodiffDir, phenopacketPath);
        }
        model.addAttribute("inputRecord", inputRecord);

        return "input";
    }

}
