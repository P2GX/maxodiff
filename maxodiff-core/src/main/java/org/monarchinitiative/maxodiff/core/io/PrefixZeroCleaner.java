package org.monarchinitiative.maxodiff.core.io;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

// Custom Deserializer
public class PrefixZeroCleaner extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null) return null;
        // Remove "OMIM:" or "HP:" prefixes and any leading zeros
        return value.replaceFirst("^(OMIM|HP):0+", "");
    }
}