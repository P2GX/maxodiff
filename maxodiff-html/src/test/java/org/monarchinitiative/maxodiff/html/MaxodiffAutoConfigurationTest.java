package org.monarchinitiative.maxodiff.html;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.html.config.MaxodiffAutoConfiguration;
import org.monarchinitiative.maxodiff.html.config.MaxodiffProperties;
import org.monarchinitiative.maxodiff.html.controller.DifferentialDiagnosisController;
import org.monarchinitiative.maxodiff.html.service.DifferentialDiagnosisEngineService;
import org.springframework.beans.factory.BeanCreationException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MaxodiffAutoConfigurationTest extends AbstractAutoConfigurationTest {

    @Test
    public void testMissingDataPath() {
        Throwable thrown = assertThrows(BeanCreationException.class, () -> load(MaxodiffAutoConfiguration.class));

        assertThat(thrown.getMessage(), containsString("Maxodiff data directory was not provided"));
    }

    @Test
    public void testBadDataPath() {
        Throwable thrown = assertThrows(BeanCreationException.class, () -> load(MaxodiffAutoConfiguration.class, "maxodiff.dataDirectory=path/to/junk"));

        assertThat(thrown.getMessage(), containsString("path/to/junk is not a directory"));
    }

    @Test
    public void testDataPath() {
        load(MaxodiffAutoConfiguration.class, "maxodiff.dataDirectory=" + TEST_DATA);

        MaxodiffDataResolver resolver = context.getBean(MaxodiffDataResolver.class);

        assertThat(resolver.dataDirectory(), is(notNullValue()));
    }

    @Test
    public void testWeCanOverrideVaPropertyValues() {
        load(MaxodiffAutoConfiguration.class,
                "maxodiff.dataDirectory=" + TEST_DATA,
                "maxodiff.nDiseases=500",
                "maxodiff.weight=0.00123"
                );

        MaxodiffProperties properties = context.getBean(MaxodiffProperties.class);

        assertThat(properties.getDataDirectory(), equalTo(TEST_DATA.toString()));
        assertThat(properties.getnDiseases(), equalTo(500));
        assertThat(properties.getWeight(), is(closeTo(0.00123, 1e-9)));
    }

    @Test
    public void testAppIsReadyToGo() {
        load(Main.class, "maxodiff.dataDirectory=" + TEST_DATA);

        // Test that few beans are available.
        assertThat(context.getBean(BiometadataService.class), is(notNullValue()));
        assertThat(context.getBean(DifferentialDiagnosisEngineService.class), is(notNullValue()));
        assertThat(context.getBean(DifferentialDiagnosisController.class), is(notNullValue()));
    }
}
