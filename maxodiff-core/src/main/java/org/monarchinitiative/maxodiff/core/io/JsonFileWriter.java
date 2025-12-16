package org.monarchinitiative.maxodiff.core.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.monarchinitiative.maxodiff.core.analysis.refinement.RefinementResults;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class JsonFileWriter {

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
}
