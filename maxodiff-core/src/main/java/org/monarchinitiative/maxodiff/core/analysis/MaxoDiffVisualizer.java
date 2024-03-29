package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.service.MaxoDiffService;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;

public class MaxoDiffVisualizer {

    private final Map<TermId, Set<SimpleTerm>> diseaseToMaxoMap;
    private final Map<TermId, Set<SimpleTerm>> diseaseToHpoMap;

    private final List<SimpleTerm> allMaxoAnnots;
    private final List<SimpleTerm> allHpoAnnots;

    private final Map<SimpleTerm, Set<SimpleTerm>> maxoDxAnnots;
    private final Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoMap;

    public MaxoDiffVisualizer(MaxoDiffService diffService) {
        this.diseaseToMaxoMap = diffService.diseaseToMaxoMap();
        this.diseaseToHpoMap = diffService.diseaseToHpoMap();
        this.allHpoAnnots = diffService.allHpoAnnots();
        this.allMaxoAnnots = diffService.allMaxoAnnots();
        this.maxoDxAnnots = diffService.maxoDxAnnots();
        this.maxoToHpoMap = diffService.maxoToHpoMap();
    }

    /** Columns -- disease, rows -- MAXO */
    public List<List<String>> diseaseToMaxoMatrix() {
        List<List<String>> rows = new ArrayList<>();
        List<TermId> diseaseList = new ArrayList<>(diseaseToMaxoMap.keySet());
        List<String> header = new ArrayList<>();
        header.add("MAXO");
        for (var diseaseId : diseaseList) {
            header.add(diseaseId.getValue());
        }
        header.add("total");
        header.add("hpo.terms");
        rows.add(header);
        for (SimpleTerm maxoST : allMaxoAnnots) {
            List<String> row = new ArrayList<>();
            int total = 0;
            row.add(maxoST.display());
            for (var diseaseId : diseaseList) {
                var diseaseMaxoAnnots = this.diseaseToMaxoMap.get(diseaseId);
                if (diseaseMaxoAnnots.contains(maxoST)) {
                    row.add("yyy");
                    total++;
                } else {
                    row.add("nnn");
                }
            }
            row.add(String.valueOf(total));
            Set<SimpleTerm> hpoterms = this.maxoToHpoMap.get(maxoST);
            int N = hpoterms.size();
            StringBuilder sb = new StringBuilder();
            String hpotermstring;
            if (N==0) {
                hpotermstring = "";
            } else if (N > 5) {
                String hpos = hpoterms.stream()
                        .limit(5)
                        .map(SimpleTerm::display)
                        .collect(Collectors.joining("; "));
                hpotermstring = String.format("Total of %d HPO terms. %s ...", N, hpos);
            } else {
                hpotermstring = hpoterms.stream()
                        .map(SimpleTerm::display)
                        .collect(Collectors.joining("; "));
            }
            row.add(hpotermstring);
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
                    row.add("+");
                    total++;
                } else {
                    row.add("-");
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
                    int N = maxoSet.size();

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
