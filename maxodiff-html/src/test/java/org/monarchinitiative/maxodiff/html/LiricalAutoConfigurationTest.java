package org.monarchinitiative.maxodiff.html;

import org.junit.jupiter.api.Test;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.maxodiff.html.config.LiricalProperties;
import org.monarchinitiative.maxodiff.lirical.LiricalDifferentialDiagnosisEngineConfigurer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class LiricalAutoConfigurationTest extends AbstractAutoConfigurationTest {

    @Test
    public void testDataPaths() {
        load(Main.class, "maxodiff.dataDirectory=" + TEST_DATA);

        LiricalProperties properties = context.getBean(LiricalProperties.class);


        // Test if the app is ready to go.
        assertThat(context.getBean(LiricalDifferentialDiagnosisEngineConfigurer.class), is(notNullValue()));
    }

}
