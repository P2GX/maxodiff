package org.monarchinitiative.maxodiff.html.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/exomiserAnalysis")
public class ExomiserAnalysisController {

    @RequestMapping
    public String runAnalysis() {
        return "exomiserAnalysis";
    }
}
