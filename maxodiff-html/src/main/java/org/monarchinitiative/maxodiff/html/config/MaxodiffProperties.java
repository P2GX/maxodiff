package org.monarchinitiative.maxodiff.html.config;

import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

import java.nio.file.Path;

@ConfigurationProperties
// TODO: the location of the properties file should be configurable, and not set to a specific location.
@PropertySource(value="file:${user.home}/.maxodiff/maxodiff.properties", ignoreResourceNotFound=true)
public class MaxodiffProperties {

    // TODO: some fields are not global properties but can differ between analysis runs.
    // TODO: make all fields public if we decide to run this on module path (not classpath)
    @Value("${maxodiff-data-directory:${user.home}/.maxodiff/data}")
    Path maxodiffDataDir;

    @Value("${lirical-data-directory:${user.home}/.maxodiff/data/lirical}")
    Path liricalDataDir;

    @Value("${lirical-genome-build:hg38}")
    String liricalGenomeBuild;

    @Value("${lirical-transcript-database:REFSEQ}")
    TranscriptDatabase liricalTranscriptDatabase;

    @Value("${lirical-pathogenicity-threshold:0.8}")
    Float liricalPathogenicityThreshold;

    @Value("${lirical-default-variant-background-frequency:0.1}")
    Double liricalDefaultVariantBackgroundFrequency;

    @Value("${lirical-strict:true}")
    boolean liricalStrict;

    @Value("${lirical-global-mode:false}")
    boolean liricalGlobalMode;

//    @Value("${lirical-exomiser-hg19-path}")
//    String liricalExomiserHg19Path;
//
//    @Value("${lirical-exomiser-hg38-path}")
//    String liricalExomiserHg38Path;

    public Path maxodiffDataDir() {
        return maxodiffDataDir;
    }

    public Path liricalDataDir() {
        return liricalDataDir;
    }

    public String liricalGenomeBuild() {
        return liricalGenomeBuild;
    }

    public TranscriptDatabase liricalTranscriptDatabase() {
        return liricalTranscriptDatabase;
    }

    public Float liricalPathogenicityThreshold() {
        return liricalPathogenicityThreshold;
    }

    public Double liricalDefaultVariantBackgroundFrequency() {
        return liricalDefaultVariantBackgroundFrequency;
    }

    public boolean liricalStrict() {
        return liricalStrict;
    }

    public boolean liricalGlobalMode() {
        return liricalGlobalMode;
    }

//    public String liricalExomiserHg19Path() {
//        return liricalExomiserHg19Path;
//    }
//
//    public String liricalExomiserHg38Path() {
//        return liricalExomiserHg38Path;
//    }

}
