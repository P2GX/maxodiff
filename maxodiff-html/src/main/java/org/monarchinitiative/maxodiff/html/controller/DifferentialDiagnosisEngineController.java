package org.monarchinitiative.maxodiff.html.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
@SessionAttributes({"engineName"})
public class DifferentialDiagnosisEngineController {


    @GetMapping("/engineInput")
    public String getInput(@RequestParam(value = "engineName", required = false) String engineName,
                            Model model) {

        if (engineName == null) {
            engineName = "lirical";
        }

        model.addAttribute("engineName", engineName);

        return "engineInput";
    }


}
