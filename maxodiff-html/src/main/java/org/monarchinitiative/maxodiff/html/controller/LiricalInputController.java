package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbabilities;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.lirical.LiricalDifferentialDiagnosisEngineConfigurer;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
public class LiricalInputController {

    private final LiricalDifferentialDiagnosisEngineConfigurer liricalDifferentialDiagnosisEngineConfigurer;
    private final HpoDiseases hpoDiseases;

    public LiricalInputController(LiricalDifferentialDiagnosisEngineConfigurer liricalDifferentialDiagnosisEngineConfigurer,
                                  HpoDiseases hpoDiseases) {
        this.liricalDifferentialDiagnosisEngineConfigurer = liricalDifferentialDiagnosisEngineConfigurer;
        this.hpoDiseases = hpoDiseases;
    }


    @RequestMapping("/liricalInput")
    public String liricalInput(
            @RequestParam(value = "genomeBuild", required = false) String genomeBuild,
            @RequestParam(value = "transcriptDatabase", required = false) TranscriptDatabase transcriptDatabase,
            @RequestParam(value = "pathogenicityThreshold", required = false) Float pathogenicityThreshold,
            @RequestParam(value = "defaultVariantBackgroundFrequency", required = false) Double defaultVariantBackgroundFrequency,
            @RequestParam(value = "strict", required = false) boolean strict,
            @RequestParam(value = "globalAnalysisMode", required = false) boolean globalAnalysisMode,
            Model model) {

        model.addAttribute("genomeBuild", genomeBuild);
        model.addAttribute("transcriptDatabase", transcriptDatabase);
        model.addAttribute("pathogenicityThreshold", pathogenicityThreshold);
        model.addAttribute("defaultVariantBackgroundFrequency", defaultVariantBackgroundFrequency);
        model.addAttribute("strict", strict);
        model.addAttribute("globalAnalysisMode", globalAnalysisMode);

        if (genomeBuild != null) {
            AnalysisOptions options = AnalysisOptions.builder()
                    .genomeBuild(GenomeBuild.valueOf(genomeBuild))
                    .transcriptDatabase(transcriptDatabase)
                    .variantDeleteriousnessThreshold(pathogenicityThreshold)
                    .defaultVariantBackgroundFrequency(defaultVariantBackgroundFrequency)
                    .useStrictPenalties(strict)
                    .useGlobal(globalAnalysisMode)
                    .pretestProbability(PretestDiseaseProbabilities.uniform(hpoDiseases.diseaseIds()))
                    .build();

            System.out.println(options);

            DifferentialDiagnosisEngine engine = liricalDifferentialDiagnosisEngineConfigurer.configure(options);
            model.addAttribute("engine", engine);
        }

        return "liricalInput";
    }

}
