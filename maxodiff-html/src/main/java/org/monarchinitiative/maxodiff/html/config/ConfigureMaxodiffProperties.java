package org.monarchinitiative.maxodiff.html.config;

import org.monarchinitiative.maxodiff.html.analysis.DownloadData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ConfigureMaxodiffProperties {

    // TODO: this should not be run when the maxodiff-html JAR is run.
    //  There must be just one place where we set up the resources for the entire app, including CLI.
    //  I think we can do it similarly to LIRICAL, where there is a download command in the CLI (not HTML module).

    private ConfigureMaxodiffProperties() {}

    public static void addToPropertiesFile(String key, String value) throws IOException {
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

    public static Path createDataDirectory(MaxodiffConfig config,
                                           MaxodiffProperties maxodiffProperties,
                                           String type) throws Exception {

        Path downloadDir = maxodiffProperties.maxodiffDataDir();
        if (type.equals("lirical")) {
            downloadDir = maxodiffProperties.liricalDataDir();
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
