package org.p2gx.maxodiff.core.io;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.MySimpleTerm;


import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MaxoDxAnnots {

    private MaxoDxAnnots() {}

    /**
     * TODO DOCUMENT ME. THIS IS THE CORRECT METHOD BUT IT WAS LISTED AS DEPRECATED!
     */
    public static Map<MySimpleTerm, Set<MySimpleTerm>> parseHpoToMaxo(BufferedReader reader) throws IOException {
        Map<MySimpleTerm, Set<MySimpleTerm>> hpoToMaxo = new HashMap<>();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("#")) continue;
            if (line.startsWith("hpo_id")) continue;
            String[] fields = line.split("\t");
            if (fields.length != 6) {
                System.err.printf("Malformed line with %d fields (expected 6): %s", fields.length, line);
                continue;
            }
            TermId hpoid = TermId.of(fields[0]);
            String hpoLabel = fields[1];
            MySimpleTerm hterm = new MySimpleTerm(hpoid, hpoLabel);
            String predicate = fields[2];
            if (predicate.equals("is_observable_through")) {
                TermId maxoId = TermId.of(fields[3]);
                String maxoLabel = fields[4];
                MySimpleTerm mterm = new MySimpleTerm(maxoId, maxoLabel);
                hpoToMaxo.computeIfAbsent(hterm, whatever -> new HashSet<>()).add(mterm);
            } else if (! predicate.equals("is_prenatally_observable_through")) {
                // skip prenatal for current analysis
                throw new RuntimeException(String.format("Did not recognize predicate %s", predicate));
            }
        }

        return hpoToMaxo;
    }

}
