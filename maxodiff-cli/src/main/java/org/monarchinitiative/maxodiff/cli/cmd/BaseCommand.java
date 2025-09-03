package org.monarchinitiative.maxodiff.cli.cmd;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
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


    @CommandLine.Option(
            names={"-d","--data"},
            description ="directory to download data (default: ${DEFAULT-VALUE})"
    )
    public Path datadir= Path.of("data");


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


}
