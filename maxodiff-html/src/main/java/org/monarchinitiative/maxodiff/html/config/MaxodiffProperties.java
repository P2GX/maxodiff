package org.monarchinitiative.maxodiff.html.config;

import org.monarchinitiative.maxodiff.html.analysis.DownloadData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
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

    public Path maxodiffDataDir() {
        return maxodiffDataDir;
    }


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

    public Path createDataDirectory() throws Exception {
        Path downloadDir = maxodiffDataDir();
        if (!Files.exists(downloadDir)) {
            DownloadData downloadData = new DownloadData();
            downloadData.downloadMaxodiffData(config, downloadDir);
        }
        return downloadDir;
    }

}
