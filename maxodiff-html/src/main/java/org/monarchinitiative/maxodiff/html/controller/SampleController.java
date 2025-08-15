package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.maxodiff.lirical.PhenopacketFileParser;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/sampleInput")
@SessionAttributes({"sample"})
public class SampleController {

    private static final Path UPLOAD_DIR = Paths.get(System.getProperty("user.home"), "maxodiff", "uploads");


    @RequestMapping
    public String sampleInput(@RequestParam(value = "id", required = false) String id,
                              @RequestParam(value = "presentHpoTermIds", required = false) String presentHpoTermIds,
                              @RequestParam(value = "excludedHpoTermIds", required = false) String excludedHpoTermIds,
                              Model model) {


        model.addAttribute("id", id);
        model.addAttribute("presentHpoTermIds", presentHpoTermIds);
        model.addAttribute("excludedHpoTermIds", excludedHpoTermIds);

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

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {

        Map<String, String> result = new HashMap<>();
        try {

            if (!Files.exists(UPLOAD_DIR)) {
                Files.createDirectories(UPLOAD_DIR);
            }

            Path phenopacketPath = UPLOAD_DIR.resolve(Objects.requireNonNull(file.getOriginalFilename()));
            file.transferTo(phenopacketPath.toFile());

            String sampleId = "";
            String presentHpoTermIds = "";
            String excludedHpoTermIds = "";
            if (phenopacketPath != null) {
                PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(phenopacketPath);
                sampleId = phenopacketData.sampleId();
                presentHpoTermIds = phenopacketData.presentHpoTermIds().map(Object::toString).collect(Collectors.joining(","));
                excludedHpoTermIds = phenopacketData.excludedHpoTermIds().map(Object::toString).collect(Collectors.joining(","));
            }

            String phenopacketName = file.getOriginalFilename();

            result.put("phenopacketName", phenopacketName);
            result.put("id", sampleId);
            result.put("presentHpoTermIds", presentHpoTermIds);
            result.put("excludedHpoTermIds", excludedHpoTermIds);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
