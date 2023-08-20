package org.monarchinitiative.maxodiff.analysis;

import org.monarchinitiative.maxodiff.model.SimpleTerm;
import org.monarchinitiative.maxodiff.service.MaxoDiffService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MaxoDiffVisualizer {

    private final Map<TermId, Set<SimpleTerm>> diseaseToMaxoMap;
    private final Map<TermId, Set<SimpleTerm>> diseaseToHpoMap;

    private final List<SimpleTerm> allMaxoAnnots;
    private final List<SimpleTerm> allHpoAnnots;

    Map<SimpleTerm, Set<SimpleTerm>> maxoDxAnnots;

    public MaxoDiffVisualizer(MaxoDiffService diffService) {
        this.diseaseToMaxoMap = diffService.diseaseToMaxoMap();
        this.diseaseToHpoMap = diffService.diseaseToHpoMap();
        this.allHpoAnnots = diffService.allHpoAnnots();
        this.allMaxoAnnots = diffService.allMaxoAnnots();
        this.maxoDxAnnots = diffService.maxoDxAnnots();
    }

    /** Columns -- disease, rows -- MAXO */
    public List<List<String>> diseaseToMaxoMatrix() {
        List<List<String>> rows = new ArrayList<>();
        List<TermId> diseaseList = new ArrayList<>(diseaseToMaxoMap.keySet());
        List<String> header = new ArrayList<>();
        header.add("MAXO.label");
        header.add("MAXO.id");
        for (var diseaseId : diseaseList) {
            header.add(diseaseId.getValue());
        }
        header.add("total");
        rows.add(header);
        for (SimpleTerm maxoST : allMaxoAnnots) {
            List<String> row = new ArrayList<>();
            int total = 0;
            row.add(maxoST.label());
            row.add(maxoST.tid().getValue());
            for (var diseaseId : diseaseList) {
                var diseaseMaxoAnnots = this.diseaseToMaxoMap.get(diseaseId);
                if (diseaseMaxoAnnots.contains(maxoST)) {
                    row.add("yes");
                    total++;
                } else {
                    row.add("");
                }
            }
            row.add(String.valueOf(total));
            rows.add(row);
        }
        return rows;
    }

    /** Columns -- disease, rows -- MAXO */
    public List<List<String>> diseaseToHpoMatrix() {
        List<List<String>> rows = new ArrayList<>();
        List<TermId> diseaseList = new ArrayList<>(diseaseToHpoMap.keySet());
        List<String> header = new ArrayList<>();
        header.add("HPO.label");
        header.add("HPO.id");
        for (var diseaseId : diseaseList) {
            header.add(diseaseId.getValue());
        }
        header.add("total");
        rows.add(header);
        for (SimpleTerm hpoST : allHpoAnnots) {
            List<String> row = new ArrayList<>();
            int total = 0;
            row.add(hpoST.label());
            row.add(hpoST.tid().getValue());
            for (var diseaseId : diseaseList) {
                var diseaseMaxoAnnots = this.diseaseToMaxoMap.get(diseaseId);
                if (diseaseMaxoAnnots.contains(hpoST)) {
                    row.add("yes");
                    total++;
                } else {
                    row.add("");
                }
            }
            row.add(String.valueOf(total));
            rows.add(row);
        }
        return rows;
    }


    public List<String> diseaseToHpoMaxo() {
        List<String> rows = new ArrayList<>();
        for (var entry:diseaseToHpoMap.entrySet()) {
            TermId diseaseId = entry.getKey();
            Set<SimpleTerm> hpoSet = entry.getValue();
            StringBuilder sb = new StringBuilder();
            sb.append(diseaseId.getValue());
            sb.append(": ");
            for (var hpost : hpoSet) {
                if (maxoDxAnnots.containsKey(hpost)) {
                    Set<SimpleTerm> maxoSet = maxoDxAnnots.get(hpost);
                    for (var maxost: maxoSet) {
                        sb.append(String.format("%s - %s; ", hpost.display(), maxost.display()));
                    }
                }
            }
            rows.add(sb.toString());
        }
        return rows;
    }



}
