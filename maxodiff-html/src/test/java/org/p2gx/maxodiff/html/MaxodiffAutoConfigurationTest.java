package org.p2gx.maxodiff.html;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.p2gx.maxodiff.config.MaxodiffDataException;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.io.impl.MdContextBuilder;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.p2gx.maxodiff.html.config.MaxodiffAutoConfiguration;
import org.p2gx.maxodiff.html.config.MaxodiffProperties;
import org.p2gx.maxodiff.html.controller.MaxodiffController;
import org.p2gx.maxodiff.html.service.DifferentialDiagnosisEngineService;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MaxodiffAutoConfigurationTest extends AbstractAutoConfigurationTest {

    @TestConfiguration
    @EnableConfigurationProperties({MaxodiffProperties.class})
    public static class MaxodiffTestAutoConfiguration {

        @Bean
        public Path maxodiffDataDirectory(MaxodiffProperties maxodiffProperties) throws MaxodiffDataException {
            if (maxodiffProperties.getDataDirectory() == null) {
                throw new MaxodiffDataException("Maxodiff data directory was not provided");
            }
            Path dataDirectory = Path.of(maxodiffProperties.getDataDirectory());
            if (!Files.isDirectory(dataDirectory)) {
                throw new MaxodiffDataException("%s is not a directory".formatted(dataDirectory.toAbsolutePath()));
            }
            return dataDirectory;
        }

        @Bean
        public MdContext mdContext(Path maxodiffDataDirectory) throws Exception {
            return MdContextBuilder.buildTestContext(maxodiffDataDirectory, 100, 20);
        }
    }


    @Test
    public void testMissingDataPath() {
        Throwable thrown = assertThrows(BeanCreationException.class, () -> load(MaxodiffAutoConfiguration.class));

        assertThat(thrown.getMessage(), containsString("Maxodiff data directory was not provided"));
    }

    @Test
    public void testBadDataPath() {
        Throwable thrown = assertThrows(BeanCreationException.class, () -> load(MaxodiffAutoConfiguration.class, "maxodiff.data-directory=path/to/junk"));

        assertThat(thrown.getMessage(), containsString(String.join(File.separator,"path", "to", "junk") + " is not a directory"));
    }

    @Test
    public void testHpoLoaded() {
        load(MaxodiffTestAutoConfiguration.class, "maxodiff.data-directory=" + TEST_DATA);

        MdContext mdContext = context.getBean(MdContext.class);

        assertThat(mdContext.resources().hpo(), is(notNullValue()));
    }

    @Test
    public void testWeCanOverrideVaPropertyValues() {
        load(MaxodiffTestAutoConfiguration.class,
                "maxodiff.data-directory=" + TEST_DATA,
                "maxodiff.n-diseases=500",
                "maxodiff.weight=0.00123"
                );

        MaxodiffProperties properties = context.getBean(MaxodiffProperties.class);

        assertThat(properties.getDataDirectory(), equalTo(TEST_DATA.toString()));
        assertThat(properties.getnDiseases(), equalTo(500));
        assertThat(properties.getWeight(), is(closeTo(0.00123, 1e-9)));
    }

    @Test
    @Disabled
    public void testAppIsReadyToGo() {
        load(Main.class, "maxodiff.data-directory=" + TEST_DATA);

        // Test that few beans are available.
        assertThat(context.getBean(BiometadataService.class), is(notNullValue()));
        MatcherAssert.assertThat(context.getBean(DifferentialDiagnosisEngineService.class), is(notNullValue()));
        MatcherAssert.assertThat(context.getBean(MaxodiffController.class), is(notNullValue()));
    }
}
