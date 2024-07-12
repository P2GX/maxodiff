package org.monarchinitiative.maxodiff.html.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/nullAnalysis")
public class NullAnalysisController {

    @RequestMapping
    public String runAnalysis() {
        return "nullAnalysis";
    }

    @RequestMapping("/engineInput")
    public String engineInput() {
        return "engineInput";
    }

}
