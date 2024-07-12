package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Controller
@RequestMapping("/liricalAnalysis")
@SessionAttributes({"options", "sample", "differentialDiagnoses"})
public class LiricalAnalysisController {

    @RequestMapping
    public String liricalAnalysis(
            @SessionAttribute(value = "engine", required = false) DifferentialDiagnosisEngine engine,
            @SessionAttribute(value = "sample", required = false) Sample sample,
            Model model) {


        List<DifferentialDiagnosis> differentialDiagnoses = List.of();

        if (engine != null && sample != null) {
            // Get initial differential diagnoses from running LIRICAL
            differentialDiagnoses = engine.run(sample);
        }
        model.addAttribute("differentialDiagnoses", differentialDiagnoses);

        return "liricalAnalysis";
    }

}
