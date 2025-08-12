package org.monarchinitiative.maxodiff.cli.cmd;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.monarchinitiative.lirical.configuration.LiricalBuilder;
import org.monarchinitiative.lirical.core.Lirical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Base class that describes data and configuration sections of the CLI, and contains common functionalities.
 */
abstract class BaseCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseCommand.class);

    protected static final String BANNER = readBanner();

    static String readBanner() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(BaseCommand.class.getResourceAsStream("/banner.txt")), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            // swallow
            return "";
        }
    }

    // ---------------------------------------------- LOGGING VERBOSITY ------------------------------------------------
    @CommandLine.Option(names = {"-v"},
            description = {"Specify multiple -v options to increase verbosity.",
                    "For example, `-v -v -v` or `-vvv`"})
    public boolean[] verbosity = {};



    // ---------------------------------------------- RESOURCES --------------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading = "Resource paths:%n")
    public DataSection dataSection = new DataSection();

    public static class DataSection {
        @CommandLine.Option(names = {"-d", "--data"},
                description = "Path to Lirical data directory.")
        public Path liricalDataDirectory = Path.of("data", "lirical");

    }


    // ---------------------------------------------- CONFIGURATION ----------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading = "Configuration options:%n")
    public RunConfiguration runConfiguration = new RunConfiguration();

    public static class RunConfiguration {
        /**
         * If global is set to true, then LIRICAL will not discard candidate diseases with no known disease gene or
         * candidates for which no predicted pathogenic variant was found in the VCF.
         */
        @CommandLine.Option(names = {"-g", "--global"},
                description = "Global analysis (default: ${DEFAULT-VALUE}).")
        public boolean globalAnalysisMode = false;

        @CommandLine.Option(names = {"--strict"},
                description = "Use strict penalties if the genotype does not match the disease model in terms " +
                        "of number of called pathogenic alleles. (default: ${DEFAULT-VALUE}).")
        public boolean strict = false;

    }

    public Integer call() throws Exception {
        // (0) Set up verbosity and print banner.
        setupLoggingAndPrintBanner();

        // (1) Run the command functionality
        return execute();
    }

    protected abstract Integer execute() throws Exception;

    void setupLoggingAndPrintBanner() {
        Level level = parseVerbosityLevel();

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(level);

        printBanner();
    }

    private Level parseVerbosityLevel() {
        int verbosity = 0;
        for (boolean a : this.verbosity) {
            if (a) verbosity++;
        }

        return switch (verbosity) {
            case 0 -> Level.INFO;
            case 1 -> Level.DEBUG;
            case 2 -> Level.TRACE;
            default -> Level.ALL;
        };
    }

    private static void printBanner() {
        System.err.println(readBanner());
    }

    protected Lirical prepareLirical() throws Exception {
        // Check input.
        Collection<String> errors = checkInput();
        if (!errors.isEmpty())
            throw new Exception(String.format("Errors: %s", String.join(", ", errors)));

        // Bootstrap LIRICAL.
        return bootstrapLirical();
    }

    protected Collection<String> checkInput() {
        Collection<String> errors = new LinkedList<>();
        // resources
        if (dataSection.liricalDataDirectory == null) {
            String msg = "Path to Lirical data directory must be provided via `-d | --data` option";
            LOGGER.error(msg);
            errors.add(msg);
        }
        return errors;
    }

    protected Lirical bootstrapLirical() throws Exception {
        Properties properties = readProperties();
        String liricalVersion = properties.getProperty("lirical.version", "unknown version");
        LOGGER.info("Spooling up Lirical v{}", liricalVersion);
        return LiricalBuilder.builder(dataSection.liricalDataDirectory)
                .build();

    }

    private static Properties readProperties() {
        Properties properties = new Properties();

        try (InputStream is = BaseCommand.class.getResourceAsStream("/lirical.properties")) {
            properties.load(is);
        } catch (IOException e) {
            LOGGER.warn("Error loading properties: {}", e.getMessage());
        }
        return properties;
    }

}
