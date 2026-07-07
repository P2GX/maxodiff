package org.p2gx.maxodiff.core.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResultSingleDisease;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class JsonWriter {

    public static void writeModalitiesToJsonFile(Path filePath, List<RankedMaxoResultSingleDisease> results) throws IOException {
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