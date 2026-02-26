package org.monarchinitiative.maxodiff.core.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.monarchinitiative.maxodiff.core.analysis.RankedMaxoResult;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementResults;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class JsonWriter {

    public static void writeToJsonFile(Path filePath, RefinementResults results) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
        writer.writeValue(new File(filePath.toString()), results);
    }

    public static void writeToJsonFile(Path filePath, String results) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
        writer.writeValue(new File(filePath.toString()), results);
    }

    public static void writeToJsonFile(Path filePath, List<RankedMaxoResult> results) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
        writer.writeValue(new File(filePath.toString()), results);
    }

    public static String writeToJsonString(List<RankedMaxoResult> results) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(results);
    }
}