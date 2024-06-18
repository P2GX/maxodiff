package org.monarchinitiative.maxodiff.html.config;

import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

@ConfigurationProperties
// TODO: the location of the properties file should be configurable, and not set to a specific location.
@PropertySource(value="classpath:/maxodiff-config/src/main/resources/maxodiff.lirical.properties", ignoreResourceNotFound=true)
public class MaxodiffLiricalProperties {

    @Value("${genome-build:hg38}")
    String genomeBuild;

    @Value("${transcript-database:REFSEQ}")
    TranscriptDatabase transcriptDatabase;

    @Value("${pathogenicity-threshold:0.8}")
    Float pathogenicityThreshold;

    @Value("${default-variant-background-frequency:0.1}")
    Double defaultVariantBackgroundFrequency;

    @Value("${strict:true}")
    boolean strict;

    @Value("${global-analysis-mode:false}")
    boolean globalMode;

//    @Value("${lirical-exomiser-hg19-path}")
//    String liricalExomiserHg19Path;
//
//    @Value("${lirical-exomiser-hg38-path}")
//    String liricalExomiserHg38Path;

    public String genomeBuild() {
        return genomeBuild;
    }

    public TranscriptDatabase transcriptDatabase() {
        return transcriptDatabase;
    }

    public Float pathogenicityThreshold() {
        return pathogenicityThreshold;
    }

    public Double defaultVariantBackgroundFrequency() {
        return defaultVariantBackgroundFrequency;
    }

    public boolean strict() {
        return strict;
    }

    public boolean globalMode() {
        return globalMode;
    }

//    public String liricalExomiserHg19Path() {
//        return liricalExomiserHg19Path;
//    }
//
//    public String liricalExomiserHg38Path() {
//        return liricalExomiserHg38Path;
//    }

}
