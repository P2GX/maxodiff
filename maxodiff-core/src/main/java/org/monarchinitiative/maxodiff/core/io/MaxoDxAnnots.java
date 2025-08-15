package org.monarchinitiative.maxodiff.core.io;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MaxoDxAnnots {

    private MaxoDxAnnots() {}

    /**
     * @deprecated use {@link #parseMaxoToHpo(BufferedReader)} instead
     */
    @Deprecated
    public static Map<SimpleTerm, Set<SimpleTerm>> parseHpoToMaxo(BufferedReader reader) throws IOException {
        Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxo = new HashMap<>();

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
            SimpleTerm hterm = new SimpleTerm(hpoid, hpoLabel);
            String predicate = fields[2];
            if (predicate.equals("is_observable_through")) {
                TermId maxoId = TermId.of(fields[3]);
                String maxoLabel = fields[4];
                SimpleTerm mterm = new SimpleTerm(maxoId, maxoLabel);
                hpoToMaxo.computeIfAbsent(hterm, whatever -> new HashSet<>()).add(mterm);
            } else if (predicate.equals("is_prenatally_observable_through")) {
                continue; // skip prenatal for this analysis
            } else {
                throw new RuntimeException(String.format("Did not recognize predicate %s", predicate));
            }
        }

        return hpoToMaxo;
    }

    public static Map<SimpleTerm, Set<SimpleTerm>> parseMaxoToHpo(BufferedReader reader) throws IOException {
        Map<SimpleTerm, Set<SimpleTerm>> maxoToHpo = new HashMap<>();

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
            SimpleTerm hterm = new SimpleTerm(hpoid, hpoLabel);
            String predicate = fields[2];
            if (predicate.equals("is_observable_through")) {
                TermId maxoId = TermId.of(fields[3]);
                String maxoLabel = fields[4];
                SimpleTerm mterm = new SimpleTerm(maxoId, maxoLabel);
                maxoToHpo.computeIfAbsent(mterm, whatever -> new HashSet<>()).add(hterm);
            } else if (predicate.equals("is_prenatally_observable_through")) {
                continue; // skip prenatal for this analysis
            } else {
                throw new RuntimeException(String.format("Did not recognize predicate %s", predicate));
            }
        }

        return maxoToHpo;
    }
}
