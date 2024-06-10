package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.maxodiff.core.analysis.LiricalAnalysis;
import org.monarchinitiative.maxodiff.core.analysis.LiricalAnalysis.LiricalRecord;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermMap;
import org.monarchinitiative.maxodiff.html.analysis.InputRecord;
import org.monarchinitiative.maxodiff.html.config.ConfigureMaxodiffProperties;
import org.monarchinitiative.maxodiff.html.config.MaxodiffConfig;
import org.monarchinitiative.maxodiff.html.config.MaxodiffProperties;
import org.monarchinitiative.maxodiff.html.service.SessionResultsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;


@Controller
@RequestMapping("/input")
@SessionAttributes({"liricalRecord", "inputRecord"})
public class InputController {

    // TODO: we should not need to use the `config` at this place.
    //  We need to define an interface for the "analyzer",
    //  implement it e.g. using LIRICAL or Exomiser,
    //  and use the interface by this controller
    //  (or write a service that uses the "analyzer" interface and use the service in the controller)
    @Autowired
    private MaxodiffConfig config;

    // TODO: similarly to `config`, I think we should not use `properties` at this place.
    //  When the controller is created, the app should already be set up.
    @Autowired
    private MaxodiffProperties properties;

    @Autowired
    SessionResultsService sessionResultsService = new SessionResultsService();


    @RequestMapping
    public String input(
            // TODO: LIRICAL data dir should not be here.
            @RequestParam(value = "liricalDataDir", required = false) Path liricalDataDir,
            @RequestParam(value = "genomeBuild", required = false) String genomeBuild,
            @RequestParam(value = "transcriptDatabase", required = false) TranscriptDatabase transcriptDatabase,
            @RequestParam(value = "pathogenicityThreshold", required = false) Float pathogenicityThreshold,
            @RequestParam(value = "defaultVariantBackgroundFrequency", required = false) Double defaultVariantBackgroundFrequency,
            @RequestParam(value = "strict", required = false) boolean strict,
            @RequestParam(value = "globalAnalysisMode", required = false) boolean globalAnalysisMode,
            // TODO: Exomiser path should not be defined here.
            @RequestParam(value = "exomiserPath", required = false) Path exomiserPath,
            @RequestParam(value = "vcfPath", required = false) Path vcfPath,
            @RequestParam(value = "maxodiffDir", required = false) Path maxodiffDir,
            @RequestParam(value = "phenopacketPath", required = false) Path phenopacketPath,
            Model model) throws Exception {

        //Populate fields with default values
        if (maxodiffDir == null) {
            maxodiffDir = ConfigureMaxodiffProperties.createDataDirectory(config, properties, "maxodiff");
            ConfigureMaxodiffProperties.addToPropertiesFile("maxodiff-data-directory", maxodiffDir.toString());
        }
        if (liricalDataDir == null) {
            liricalDataDir = ConfigureMaxodiffProperties.createDataDirectory(config, properties, "lirical");
            ConfigureMaxodiffProperties.addToPropertiesFile("lirical-data-directory", liricalDataDir.toString());
        }
        if (genomeBuild == null) {
            genomeBuild = properties.liricalGenomeBuild();
            ConfigureMaxodiffProperties.addToPropertiesFile("lirical-genome-build", genomeBuild);
        }
        if (transcriptDatabase == null) {
            transcriptDatabase = properties.liricalTranscriptDatabase();
            ConfigureMaxodiffProperties.addToPropertiesFile("lirical-transcript-database", transcriptDatabase.toString().toUpperCase());
        }
        if (pathogenicityThreshold == null) {
            pathogenicityThreshold = properties.liricalPathogenicityThreshold();
            ConfigureMaxodiffProperties.addToPropertiesFile("lirical-pathogenicity-threshold", pathogenicityThreshold.toString());
        }
        if (defaultVariantBackgroundFrequency == null) {
            defaultVariantBackgroundFrequency = properties.liricalDefaultVariantBackgroundFrequency();
            ConfigureMaxodiffProperties.addToPropertiesFile("lirical-default-variant-background-frequency", defaultVariantBackgroundFrequency.toString());
        }
        if (!strict) {
            strict = properties.liricalStrict();
            ConfigureMaxodiffProperties.addToPropertiesFile("lirical-strict", String.valueOf(strict));
        }

        //Run LIRICAL calculation and add records to model
        // TODO: at this place, we need to use the "analysis" service
        //  instead of hard-coded LIRICAL
        MaxoTermMap maxoTermMap = new MaxoTermMap(maxodiffDir);
        LiricalRecord liricalRecord = new LiricalRecord(genomeBuild, transcriptDatabase, pathogenicityThreshold,
                defaultVariantBackgroundFrequency, strict, globalAnalysisMode, liricalDataDir, exomiserPath, vcfPath);
        model.addAttribute("liricalRecord", liricalRecord);
        AnalysisResults liricalResults;
        if (phenopacketPath != null) {
            LiricalAnalysis liricalAnalysis = new LiricalAnalysis(liricalRecord);
            liricalResults = liricalAnalysis.runLiricalAnalysis(phenopacketPath);
        } else {
            // TODO: improve error handling
            throw new RuntimeException("Cannot proceed without LIRICAL results");
        }
        InputRecord inputRecord = new InputRecord(maxodiffDir, maxoTermMap, liricalResults, phenopacketPath);
        model.addAttribute("inputRecord", inputRecord);
        return "input";
    }

}
