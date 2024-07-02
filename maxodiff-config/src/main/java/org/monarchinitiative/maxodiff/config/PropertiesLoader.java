package org.monarchinitiative.maxodiff.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

/**
 * @deprecated I am not convinced this is what we want to do in the config. 
 * We are forcing the users to use {@link Properties} - we are making this decision on their behalf.
 * The better way is to write a builder class. The builder has default values for <em>optional</em> parameters, 
 * and it either asks the user to provide the <em>required</em> parameters when instantiated (constructor), 
 * or it fails to build unless the parameters are provided. 
 * See <code>AnalysisOptions.Builder</code> in LIRICAL for an example of this pattern.
 */
@Deprecated(forRemoval = true)
public class PropertiesLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesLoader.class);

    public static Properties loadProperties(String filename) {
        Properties configuration = new Properties();
        try(InputStream inputStream = Files.newInputStream(Paths.get(filename))) {
            configuration.load(inputStream);
        } catch (IOException e) {
            LOGGER.warn("Error loading properties: {}", e.getMessage());
            e.printStackTrace();
        }
        return configuration;
    }

    public static void addToPropertiesFile(String filename, String key, String value) {
        try {
            InputStream in = Files.newInputStream(Paths.get(filename));
            Properties props = new Properties();
            props.load(in);
            in.close();

            FileOutputStream out = new FileOutputStream(filename);

            LOGGER.info("adding " + key + "=" + value + " to " + filename);
            props.setProperty(key, value);
            props.store(out, null);
            out.close();

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    public static String getPropertiesFilepath(String filename) {
        String path = Objects.requireNonNull(PropertiesLoader.class.getClassLoader().getResource(filename)).getPath();

        String parent = path.split("maxodiff")[0];
        if (parent.contains(":")) {
            parent = parent.split(":")[1];
        }
        parent += "maxodiff";
        return String.join(File.separator, parent, "maxodiff-config", "src", "main", "resources", filename);
    }

}

