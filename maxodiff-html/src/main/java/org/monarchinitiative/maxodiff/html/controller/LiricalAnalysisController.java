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
@RequestMapping("/liricalAnalysis")
@SessionAttributes({"options", "sample", "differentialDiagnoses"})
public class LiricalAnalysisController {

    @RequestMapping
    public String liricalAnalysis(
            @SessionAttribute("engine") DifferentialDiagnosisEngine engine,
            @SessionAttribute("sample") Sample sample,
            Model model) {


        List<DifferentialDiagnosis> differentialDiagnoses = List.of();

        if (sample != null) {
            // Get initial differential diagnoses from running LIRICAL
            differentialDiagnoses = engine.run(sample);
        }
        model.addAttribute("differentialDiagnoses", differentialDiagnoses);

        return "liricalAnalysis";
    }

}
