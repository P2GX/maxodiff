package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.html.service.DifferentialDiagnosisEngineServiceImpl;
import org.monarchinitiative.maxodiff.lirical.LiricalDifferentialDiagnosisEngine;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
//@RequestMapping("/engineInput")
@SessionAttributes({"engine", "engineName"})
public class DifferentialDiagnosisEngineController {

    private final DifferentialDiagnosisEngineServiceImpl differentialDiagnosisEngineServiceImpl;

    public DifferentialDiagnosisEngineController(
            DifferentialDiagnosisEngineServiceImpl differentialDiagnosisEngineServiceImpl) {
        this.differentialDiagnosisEngineServiceImpl = differentialDiagnosisEngineServiceImpl;
    }

    public record EngineRecord(String engineName) {}

    @GetMapping("/engineInput")
    public String getInput(@RequestParam(value = "engineName", required = false) String engineName,
                            Model model) {


        model.addAttribute("engineService", differentialDiagnosisEngineServiceImpl);
        List<String> engineNames = differentialDiagnosisEngineServiceImpl.getEngineNames().stream().toList();
        model.addAttribute("engineNames", engineNames);
        EngineRecord engineRecord = new EngineRecord(engineName);
        model.addAttribute("engineName", engineName);
        if (engineName == null) {
            engineRecord = new EngineRecord("lirical");
        }
        model.addAttribute("engineRecord", engineRecord);

        if (engineName != null) {
            Optional<DifferentialDiagnosisEngine> engineOptional = differentialDiagnosisEngineServiceImpl.getEngine(engineName);
            if (engineOptional.isPresent()) {
                DifferentialDiagnosisEngine engine = engineOptional.get();
                model.addAttribute("engine", engine);
                System.out.println("engine = " + engineName + ": " + engine);
            }
        }

        return "engineInput";
    }

    //TODO: post mapping is not working
    @PostMapping("/engineInput")
    public String getEngine(@ModelAttribute("engineName") String engineName, Model model) {
        if (engineName != null) {
            System.out.println("post engineName = " + engineName);
            Optional<DifferentialDiagnosisEngine> engineOptional = differentialDiagnosisEngineServiceImpl.getEngine(engineName);
            if (engineOptional.isPresent()) {
                DifferentialDiagnosisEngine engine = engineOptional.get();
                model.addAttribute("engine", engine);
                System.out.println("engine = " + engineName + ": " + engine);
            }
        }
        return engineName + "Input";
    }

}
