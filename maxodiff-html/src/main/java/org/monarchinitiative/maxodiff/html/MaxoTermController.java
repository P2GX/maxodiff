package org.monarchinitiative.maxodiff.html;

import org.monarchinitiative.maxodiff.core.analysis.DiseaseTermCountImpl;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermMap;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

@Controller
public class MaxoTermController {

    @Autowired
    MaxoTermService maxoTermService;

//    @RequestMapping("/")
//    public String getPhenopackets(@RequestParam(value = "phenopacketDir", required = false) String phenopacketDir,
//                             Model model) throws Exception {
//
//        File dir = new File(phenopacketDir);
//        List<File> phenopackets = Arrays.stream(Objects.requireNonNull(dir.listFiles((dir1, name) -> name.endsWith(".json"))))
//                .toList().stream().sorted().toList();
//        model.addAttribute("phenopackets", phenopackets);
//
//        return "index";
//    }

    @RequestMapping("/")
    public String getResults(//@RequestParam(value = "maxoDataPath", required = false) Path maxoDataPath,
                             @RequestParam(value = "phenopacketPath", required = false) Path phenopacketPath,
                             @RequestParam(value = "posttestFilter", required = false) Double posttestFilter,
                             @RequestParam(value = "weight", required = false) Double weight,
                             Model model) throws Exception {

        //maxoTermService = new MaxoTermService(maxoDataPath);
        if (phenopacketPath != null & posttestFilter != null & weight != null) {
            List<MaxoTermMap.MaxoTerm> maxoTermRecords = maxoTermService.getMaxoTermRecords(phenopacketPath, posttestFilter, weight);
            TermId diseaseId = maxoTermService.getDiseaseId();
            String phenopacketName = phenopacketPath.toFile().getName();
            model.addAttribute("phenopacket", phenopacketName);
            model.addAttribute("diseaseId", diseaseId);
            model.addAttribute("posttestFilter", posttestFilter);
            model.addAttribute("weight", weight);
            model.addAttribute("maxoRecords", maxoTermRecords);

            Map<MaxoTermMap.MaxoTerm, List<MaxoTermMap.Frequencies>> maxoTables = new LinkedHashMap<>();
            List<String> omimIds = new ArrayList<>(Arrays.asList(maxoTermRecords.get(0).omimTerms()
                    .replaceAll("\\[", "")
                    .replaceAll("\\]","")
                    .replaceAll(" ", "")
                    .split(",")));
            int nMaxoResults = 10;
            for (MaxoTermMap.MaxoTerm maxoTermRecord : maxoTermRecords.subList(0, nMaxoResults)) {
                List<MaxoTermMap.Frequencies> frequencyRecords = maxoTermService.getFrequencyRecords(maxoTermRecord);
                maxoTables.put(maxoTermRecord, frequencyRecords);
            }
            model.addAttribute("omimIds", omimIds);
            model.addAttribute("maxoTables", maxoTables);
        }
        return "index";
    }



}
