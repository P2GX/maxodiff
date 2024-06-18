package org.monarchinitiative.maxodiff.html.config;

import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

@ConfigurationProperties
// TODO: the location of the properties file should be configurable, and not set to a specific location.
@PropertySource(value="classpath:/maxodiff-config/src/main/resources/maxodiff.refiner.properties", ignoreResourceNotFound=true)
public class MaxodiffRefinerProperties {

    @Value("${n-diseases:20}")
    Integer nDiseases;

    @Value("${weight:0.5}")
    Double weight;

    @Value("${n-maxo-results:10}")
    Integer nMaxoResults;


    public Integer nDiseases() {
        return nDiseases;
    }

    public Double weight() {
        return weight;
    }

    public Integer nMaxoResults() {
        return nMaxoResults;
    }


}
