package org.monarchinitiative.maxodiff.html.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

import java.nio.file.Path;

@ConfigurationProperties
// TODO: the location of the properties file should be configurable, and not set to a specific location.
@PropertySource(value="file:${user.home}/.maxodiff/maxodiff.properties", ignoreResourceNotFound=true)
//@PropertySource(value="classpath:/maxodiff-config/src/main/resources/application.properties", ignoreResourceNotFound = true)
public class MaxodiffProperties {

    // TODO: make all fields public if we decide to run this on module path (not classpath)
    @Value("${maxodiff-data-directory:${user.home}/.maxodiff/data}")
    Path maxodiffDataDir;

    @Value("${lirical-data-directory:${user.home}/.maxodiff/data/lirical}")
    Path liricalDataDir;


    public Path maxodiffDataDir() {
        return maxodiffDataDir;
    }

    public Path liricalDataDir() {
        return liricalDataDir;
    }


}
