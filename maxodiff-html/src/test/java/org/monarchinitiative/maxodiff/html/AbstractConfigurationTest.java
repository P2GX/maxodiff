package org.monarchinitiative.maxodiff.html;

import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.nio.file.Path;

public abstract class AbstractConfigurationTest {

    protected static Path TEST_DATA = Path.of("src/test/resources/testdata");

    protected ConfigurableApplicationContext context;

    @AfterEach
    public synchronized void tearDown() {
        if (context != null)
            context.close();
    }

    protected void load(Class<?> config, String... environment) {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.register(config);
        TestPropertyValues.of(environment)
                .applyTo(ctx);
        ctx.refresh();
        this.context = ctx;
    }
}
