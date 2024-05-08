package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.maxodiff.core.analysis.LiricalAnalysis;
import org.monarchinitiative.maxodiff.core.analysis.LiricalAnalysis.LiricalRecord;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermMap;
import org.monarchinitiative.maxodiff.html.analysis.InputRecord;
import org.monarchinitiative.maxodiff.html.config.MaxodiffProperties;
import org.monarchinitiative.maxodiff.html.service.MaxoTermService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;


@Controller
@RequestMapping("/input")
@SessionAttributes({"liricalRecord", "inputRecord"})
public class InputController {

    @Autowired
    private MaxodiffProperties properties;

    @Autowired
    MaxoTermService maxoTermService = new MaxoTermService();

    AnalysisResults liricalResults;


    @RequestMapping
    public String input(@RequestParam(value = "liricalDataDir", required = false) Path liricalDataDir,
                        @RequestParam(value = "genomeBuild", required = false) String genomeBuild,
                        @RequestParam(value = "transcriptDatabase", required = false) TranscriptDatabase transcriptDatabase,
                        @RequestParam(value = "pathogenicityThreshold", required = false) Float pathogenicityThreshold,
                        @RequestParam(value = "defaultVariantBackgroundFrequency", required = false) Double defaultVariantBackgroundFrequency,
                        @RequestParam(value = "strict", required = false) boolean strict,
                        @RequestParam(value = "globalAnalysisMode", required = false) boolean globalAnalysisMode,
                        @RequestParam(value = "exomiserPath", required = false) Path exomiserPath,
                        @RequestParam(value = "vcfPath", required = false) Path vcfPath,
                        @RequestParam(value = "maxodiffDir", required = false) Path maxodiffDir,
                        @RequestParam(value = "phenopacketPath", required = false) Path phenopacketPath,
                        Model model) throws Exception {

        //Populate fields with default values
        if (maxodiffDir == null) {
            maxodiffDir = properties.createDataDirectory("maxodiff");
            properties.addToPropertiesFile("maxodiff-data-directory", maxodiffDir.toString());
        }
        if (liricalDataDir == null) {
            liricalDataDir = properties.createDataDirectory("lirical");
            properties.addToPropertiesFile("lirical-data-directory", liricalDataDir.toString());
        }
        if (genomeBuild == null) {
            genomeBuild = properties.liricalGenomeBuild();
            properties.addToPropertiesFile("lirical-genome-build", genomeBuild);
        }
        if (transcriptDatabase == null) {
            transcriptDatabase = properties.liricalTranscriptDatabase();
            properties.addToPropertiesFile("lirical-transcript-database", transcriptDatabase.toString().toUpperCase());
        }
        if (pathogenicityThreshold == null) {
            pathogenicityThreshold = properties.liricalPathogenicityThreshold();
            properties.addToPropertiesFile("lirical-pathogenicity-threshold", pathogenicityThreshold.toString());
        }
        if (defaultVariantBackgroundFrequency == null) {
            defaultVariantBackgroundFrequency = properties.liricalDefaultVariantBackgroundFrequency();
            properties.addToPropertiesFile("lirical-default-variant-background-frequency", defaultVariantBackgroundFrequency.toString());
        }
        if (!strict) {
            strict = properties.liricalStrict();
            properties.addToPropertiesFile("lirical-strict", String.valueOf(strict));
        }

        //Run LIRICAL calculation and add records to model
        MaxoTermMap maxoTermMap = new MaxoTermMap(maxodiffDir);
        LiricalRecord liricalRecord = new LiricalRecord(genomeBuild, transcriptDatabase, pathogenicityThreshold,
                defaultVariantBackgroundFrequency, strict, globalAnalysisMode, liricalDataDir, exomiserPath, vcfPath);
        model.addAttribute("liricalRecord", liricalRecord);
        if (phenopacketPath != null) {
            LiricalAnalysis liricalAnalysis = new LiricalAnalysis(liricalRecord);
            liricalResults = maxoTermService.runLiricalCalculation(maxoTermMap, liricalAnalysis, phenopacketPath);
        }
        InputRecord inputRecord = new InputRecord(maxodiffDir, maxoTermMap, liricalResults, phenopacketPath);
        model.addAttribute("inputRecord", inputRecord);
        return "input";
    }

}
