package org.p2gx.maxodiff.core.io;

import org.p2gx.maxodiff.core.SimpleTermOld;
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
    public static Map<SimpleTermOld, Set<SimpleTermOld>> parseHpoToMaxo(BufferedReader reader) throws IOException {
        Map<SimpleTermOld, Set<SimpleTermOld>> hpoToMaxo = new HashMap<>();

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
            SimpleTermOld hterm = new SimpleTermOld(hpoid, hpoLabel);
            String predicate = fields[2];
            if (predicate.equals("is_observable_through")) {
                TermId maxoId = TermId.of(fields[3]);
                String maxoLabel = fields[4];
                SimpleTermOld mterm = new SimpleTermOld(maxoId, maxoLabel);
                hpoToMaxo.computeIfAbsent(hterm, whatever -> new HashSet<>()).add(mterm);
            } else if (predicate.equals("is_prenatally_observable_through")) {
                continue; // skip prenatal for this analysis
            } else {
                throw new RuntimeException(String.format("Did not recognize predicate %s", predicate));
            }
        }

        return hpoToMaxo;
    }

    public static Map<SimpleTermOld, Set<SimpleTermOld>> parseMaxoToHpo(BufferedReader reader) throws IOException {
        Map<SimpleTermOld, Set<SimpleTermOld>> maxoToHpo = new HashMap<>();

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
            SimpleTermOld hterm = new SimpleTermOld(hpoid, hpoLabel);
            String predicate = fields[2];
            if (predicate.equals("is_observable_through")) {
                TermId maxoId = TermId.of(fields[3]);
                String maxoLabel = fields[4];
                SimpleTermOld mterm = new SimpleTermOld(maxoId, maxoLabel);
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
