package org.monarchinitiative.maxodiff.html.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.properties")
public class MaxodiffConfig {

    //Properties from application.properties file
    @Value("${maxo-diagnostic-annotations-url}")
    String maxoDiagnosticAnnotsUrl;

    @Value("${jannovar-hg19-ucsc-url}")
    String jannovarHg19UcscUrl;

    @Value("${jannovar-hg19-refseq-url}")
    String jannovarHg19RefseqUrl;

    @Value("${jannovar-hg38-ucsc-url}")
    String jannovarHg38UcscUrl;

    @Value("${jannovar-hg38-refseq-url}")
    String jannovarHg38RefseqUrl;

    @Bean
    @Primary
    public String maxoDiagnosticAnnotsUrl() {
        return maxoDiagnosticAnnotsUrl;
    }

    @Bean
    public String jannovarHg19UcscUrl() {
        return jannovarHg19UcscUrl;
    }

    @Bean
    public String jannovarHg19RefseqUrl() {
        return jannovarHg19RefseqUrl;
    }

    @Bean
    public String jannovarHg38UcscUrl() {
        return jannovarHg38UcscUrl;
    }

    @Bean
    public String jannovarHg38RefseqUrl() {
        return jannovarHg38RefseqUrl;
    }

}
