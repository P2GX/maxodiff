package org.monarchinitiative.maxodiff.io;

import org.monarchinitiative.maxodiff.model.SimpleTerm;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MaxoDxAnnots {

    private final Map<SimpleTerm, Set<SimpleTerm>> simpleTermSetMap;


    /**
     * Read TSV from annots_tsv_file
     * hpo_id	hpo_label	predicate_id	maxo_id	maxo_label	creator_id
     * @param annots_tsv_file
     */
    public MaxoDxAnnots(Path annots_tsv_file) {
        simpleTermSetMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(annots_tsv_file.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) continue;
                if (line.startsWith("hpo_id")) continue;
                String [] fields = line.split("\t");
                if (fields.length != 6) {
                    System.err.printf("Malformed line with %d fields (expected 6): %s", fields.length, line);
                    continue;
                }
                TermId hpoid = TermId.of(fields[0]);
                String hpoLabel = fields[1];
                SimpleTerm hterm = new SimpleTerm(hpoid, hpoLabel);
                // omit predicate, not relevant here
                TermId maxoId = TermId.of(fields[3]);
                String maxoLabel = fields[4];
                SimpleTerm mterm = new SimpleTerm(maxoId, maxoLabel);
                simpleTermSetMap.putIfAbsent(hterm, new HashSet<>());
                simpleTermSetMap.get(hterm).add(mterm);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Map<SimpleTerm, Set<SimpleTerm>> getSimpleTermSetMap() {
        return simpleTermSetMap;
    }
}
