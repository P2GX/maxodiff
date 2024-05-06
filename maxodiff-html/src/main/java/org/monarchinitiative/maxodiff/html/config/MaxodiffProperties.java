package org.monarchinitiative.maxodiff.html.config;

import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.maxodiff.html.analysis.DownloadData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Configuration
@ConfigurationProperties
@PropertySource(value="file:${user.home}/.maxodiff/maxodiff.properties", ignoreResourceNotFound=true)
public class MaxodiffProperties {

    @Autowired
    private MaxodiffConfig config;

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


    public void addToPropertiesFile(String key, String value) throws IOException {
        String propHomeDir = String.join(File.separator, System.getProperty("user.home"), ".maxodiff");
        String propertiesFile = String.join(File.separator, propHomeDir, "maxodiff.properties");
        Path propertiesFilepath = Path.of(propertiesFile);
        File propFile;
        if (!Files.exists(propertiesFilepath)) {
            propFile = Files.createFile(propertiesFilepath).toFile();
        } else {
            propFile = new File(propertiesFile);
        }
        FileInputStream inStream = new FileInputStream(propFile);
        Properties prop = new Properties();
        prop.load(inStream);
//        System.out.println("adding " + key + "=" + value + " to " + propFile);
        prop.setProperty(key, value);
        FileOutputStream outStream  = new FileOutputStream(propFile);
        prop.store(outStream, "");
    }

    public Path createDataDirectory(String type) throws Exception {
        Path downloadDir = maxodiffDataDir();
        if (type.equals("lirical")) {
            downloadDir = liricalDataDir();
        }
        if (!Files.exists(downloadDir)) {
            DownloadData downloadData = new DownloadData();
            if (type.equals("maxodiff")) {
                downloadData.downloadMaxodiffData(config, downloadDir);
            } else if (type.equals("lirical")) {
                downloadData.downloadLiricalData(config, downloadDir);
            }
        }
        return downloadDir;
    }

}
