package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.phenomizer.IcMicaData;
import org.monarchinitiative.maxodiff.phenomizer.PhenomizerDifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.phenomizer.ScoringMode;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import java.util.Map;


@Controller
@SessionAttributes({"engine", "scoringMode"})
public class PhenomizerInputController {

    private final IcMicaData icMicaData;
    private final HpoDiseases hpoDiseases;

    public PhenomizerInputController(IcMicaData icMicaData,
                                     HpoDiseases hpoDiseases) {
        this.icMicaData = icMicaData;
        this.hpoDiseases = hpoDiseases;
    }


    @RequestMapping("/phenomizerInput")
    public String phenomizerInput(
            @RequestParam(value = "scoringMode", required = false) ScoringMode scoringMode,
            Model model) {

            if (scoringMode == null) {
                scoringMode = ScoringMode.ONE_SIDED;
            }
            model.addAttribute("scoringMode", scoringMode);

            Map<TermPair, Double> icMicaDict = icMicaData.icMicaDict();
            DifferentialDiagnosisEngine engine = new PhenomizerDifferentialDiagnosisEngine(hpoDiseases, icMicaDict, scoringMode);

            model.addAttribute("engine", engine);
            model.addAttribute("icMicaDict", icMicaDict);

        return "phenomizerInput";
    }

}
