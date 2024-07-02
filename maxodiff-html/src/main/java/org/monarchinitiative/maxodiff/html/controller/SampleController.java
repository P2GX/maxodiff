package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.io.PhenopacketFileParser;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/sampleInput")
@SessionAttributes({"sample"})
public class SampleController {


    @RequestMapping
    public String sampleInput(//@SessionAttribute("engine") DifferentialDiagnosisEngine engine,
//                              @SessionAttribute("engineName") String engineName,
                              @RequestParam(value = "phenopacketPath", required = false) Path phenopacketPath,
                              @RequestParam(value = "id", required = false) String id,
                              @RequestParam(value = "presentHpoTermIds", required = false) String presentHpoTermIds,
                              @RequestParam(value = "excludedHpoTermIds", required = false) String excludedHpoTermIds,
                              Model model) throws Exception {


        model.addAttribute("phenopacketPath", phenopacketPath);
        model.addAttribute("id", id);
        model.addAttribute("presentHpoTermIds", presentHpoTermIds);
        model.addAttribute("excludedHpoTermIds", excludedHpoTermIds);

        if (phenopacketPath != null) {
            PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(phenopacketPath);
            if (id == null | (id != null && id.isEmpty())) {
                id = phenopacketData.sampleId();
            }
            if (presentHpoTermIds == null | (presentHpoTermIds != null && presentHpoTermIds.isEmpty())) {
                presentHpoTermIds = phenopacketData.presentHpoTermIds().map(Object::toString).collect(Collectors.joining(","));
            }
            if (excludedHpoTermIds == null | (excludedHpoTermIds != null && excludedHpoTermIds.isEmpty())) {
                excludedHpoTermIds = phenopacketData.excludedHpoTermIds().map(Object::toString).collect(Collectors.joining(","));
            }
        }

        //TODO: add other possible separators to regex
        //TODO: only add valid termIDs to list
        List<TermId> presentHpoTermIdsList = (presentHpoTermIds == null | (presentHpoTermIds != null && presentHpoTermIds.isEmpty())) ?
                List.of() : Arrays.stream(presentHpoTermIds.split("[\\s,;]+"))
                .map(String::strip)
                .map(TermId::of)
                .toList();
        List<TermId> excludedHpoTermIdsList = (excludedHpoTermIds == null | (excludedHpoTermIds != null && excludedHpoTermIds.isEmpty())) ?
                List.of() : Arrays.stream(excludedHpoTermIds.split("[\\s,;]+"))
                .map(String::strip)
                .map(TermId::of)
                .toList();

        Sample sample = Sample.of(id,
                presentHpoTermIdsList,
                excludedHpoTermIdsList);
        model.addAttribute("sample", sample);

        return "sampleInput";
    }

}
