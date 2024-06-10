package org.monarchinitiative.maxodiff.core.io;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.math.NumberUtils;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LiricalResultsFileParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalResultsFileParser.class);
    private static final CSVFormat CSV_FORMAT = CSVFormat.TDF;

    private LiricalResultsFileParser() {
    }

    public static List<DifferentialDiagnosis> read(InputStream is) throws IOException {
        List<DifferentialDiagnosis> builder = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is));
             CSVParser parser = CSV_FORMAT.parse(reader)) {
            for (CSVRecord record : parser) {
                if (NumberUtils.isParsable(record.get(0))) {
                    TermId omimId = TermId.of(record.get(2));
                    String omimLabel = record.get(1);
                    String posttestPercent = record.get(4);
                    String posttestString = posttestPercent.substring(0, posttestPercent.length()-1);
                    Double posttestProb = Double.parseDouble(posttestString) / 100;
                    Double LR = Double.parseDouble(record.get(5));
                    DifferentialDiagnosis differentialDiagnosis = DifferentialDiagnosis.of(omimId, posttestProb, LR);
                    builder.add(differentialDiagnosis);
                }
            }
        }
        return List.copyOf(builder);
    }

    public static List<DifferentialDiagnosis> read(Path path) throws IOException {
        LOGGER.debug("Reading Lirical Output File from {}", path.toAbsolutePath());
        try (InputStream is = Files.newInputStream(path)) {
            return read(is);
        }
    }


}
