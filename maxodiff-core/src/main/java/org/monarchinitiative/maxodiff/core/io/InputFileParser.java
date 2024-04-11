package org.monarchinitiative.maxodiff.core.io;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class InputFileParser {

    private final List<TermId> diseaseTermIds;


    public InputFileParser(Path inputFilePath) {
        diseaseTermIds = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) continue;
                try {
                    String [] fields = line.split("\t");
                    TermId diseaseId = TermId.of(fields[0]);
                    diseaseTermIds.add(diseaseId);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<TermId> getDiseaseTermIds() {
        return diseaseTermIds;
    }
}
