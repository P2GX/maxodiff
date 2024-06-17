package org.monarchinitiative.maxodiff.html.service;


import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.maxodiff.lirical.LiricalRecord;


import java.nio.file.Path;
import java.util.Properties;

public class InputService {

    Properties properties;
    Properties liricalProperties;

    public static InputService of(Properties properties, Properties liricalProperties) {
        return new InputService(properties, liricalProperties);
    }

    private InputService(Properties properties, Properties liricalProperties) {
        this.properties = properties;
        this.liricalProperties = liricalProperties;
    }

    public LiricalRecord getDefaultLiricalRecord() {
        Path liricalDataDir = Path.of(properties.getProperty("lirical-data-directory"));

        String genomeBuild = liricalProperties.getProperty("genome-build");
        TranscriptDatabase transcriptDatabase = TranscriptDatabase.parse(liricalProperties.getProperty("transcript-database").toUpperCase()).get();
        Float pathogenicityThreshold = Float.parseFloat(liricalProperties.getProperty("pathogenicity-threshold"));
        Double defaultVariantBackgroundFrequency = Double.parseDouble(liricalProperties.getProperty("default-variant-background-frequency"));
        boolean strict = Boolean.parseBoolean(liricalProperties.getProperty("strict"));
        boolean globalAnalysisMode = Boolean.parseBoolean(liricalProperties.getProperty("global-analysis-mode"));

        return new LiricalRecord(genomeBuild, transcriptDatabase, pathogenicityThreshold,
                defaultVariantBackgroundFrequency, strict, globalAnalysisMode, liricalDataDir, null, null);
    }
}
