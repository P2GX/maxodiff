package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;

import java.util.List;


@Controller
@RequestMapping("/phenomizerAnalysis")
@SessionAttributes({"scoringMode", "sample", "differentialDiagnoses"})
public class PhenomizerAnalysisController {

    @RequestMapping
    public String phenomizerAnalysis(
            @SessionAttribute(value = "engine", required = false) DifferentialDiagnosisEngine engine,
            @SessionAttribute(value = "sample", required = false) Sample sample,
            Model model) {


        List<DifferentialDiagnosis> differentialDiagnoses = List.of();

        if (sample != null) {
            // Get initial differential diagnoses from running Phenomizer
            differentialDiagnoses = engine.run(sample);
            System.out.println("Phenomizer analysis complete.");
        }
        model.addAttribute("differentialDiagnoses", differentialDiagnoses);

        return "phenomizerAnalysis";
    }

}
