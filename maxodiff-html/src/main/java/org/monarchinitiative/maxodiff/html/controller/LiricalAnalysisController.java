package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.html.analysis.InputRecord;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;

import java.nio.file.Path;
import java.util.List;


@Controller
@RequestMapping("/liricalAnalysis")
@SessionAttributes({"liricalRecord", "inputRecord"})
public class LiricalAnalysisController {

    @RequestMapping
    public String liricalAnalysis(
            @SessionAttribute("engine") DifferentialDiagnosisEngine engine,
            @SessionAttribute("sample") Sample sample,
            Model model) throws Exception {


        InputRecord inputRecord = new InputRecord(null, null);

        if (sample != null) {
            // Get initial differential diagnoses from running LIRICAL
            List<DifferentialDiagnosis> differentialDiagnoses = engine.run(sample);
            inputRecord = new InputRecord(sample, differentialDiagnoses);
        }
        model.addAttribute("inputRecord", inputRecord);

        return "liricalAnalysis";
    }

}
