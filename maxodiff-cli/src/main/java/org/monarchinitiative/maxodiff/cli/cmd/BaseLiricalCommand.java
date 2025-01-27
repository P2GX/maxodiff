package org.monarchinitiative.maxodiff.cli.cmd;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Base class that describes data and configuration sections of the CLI, and contains common functionalities.
 */
abstract class BaseLiricalCommand implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseLiricalCommand.class);

    protected static final String BANNER = readBanner();

    static String readBanner() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(BaseLiricalCommand.class.getResourceAsStream("/banner.txt")), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            // swallow
            return "";
        }
    }

    // ---------------------------------------------- RESOURCES --------------------------------------------------------
    @CommandLine.ArgGroup(validate = false, heading = "Resource paths:%n")
    public DataSection dataSection = new DataSection();

    public static class DataSection {
        @CommandLine.Option(names = {"-d", "--data"},
                required = true,
                description = "Path to Lirical data directory.")
        public Path liricalDataDirectory;

        @CommandLine.Option(names = {"-e", "--exomiser"},
                description = "Path to the Exomiser variant database.")
        public Path exomiserDatabase = null;
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

        @CommandLine.Option(names = {"--ddndv"},
                description = "Disregard a disease if no deleterious variants are found in the gene associated with the disease. "
                        + "Used only if running with a VCF file. (default: ${DEFAULT-VALUE})")
        public boolean disregardDiseaseWithNoDeleteriousVariants = true;

        @CommandLine.Option(names = {"--transcript-db"},
                paramLabel = "{REFSEQ,UCSC}",
                description = "Transcript database (default: ${DEFAULT-VALUE}).")
        public TranscriptDatabase transcriptDb = TranscriptDatabase.REFSEQ;

        @CommandLine.Option(names = {"--use-orphanet"},
                description = "Use Orphanet annotation data (default: ${DEFAULT-VALUE}).")
        public boolean useOrphanet = false;

        @CommandLine.Option(names = {"--strict"},
                description = "Use strict penalties if the genotype does not match the disease model in terms " +
                        "of number of called pathogenic alleles. (default: ${DEFAULT-VALUE}).")
        public boolean strict = false;

        /* Default frequency of called-pathogenic variants in the general population (gnomAD). In the vast majority of
         * cases, we can derive this information from gnomAD. This constant is used if for whatever reason,
         * data was not available.
         */
        @CommandLine.Option(names = {"--variant-background-frequency"},
                // TODO - add better description
                description = "Default background frequency of variants in a gene (default: ${DEFAULT-VALUE}).")
        public double defaultVariantBackgroundFrequency = 0.1;

        @CommandLine.Option(names = {"--pathogenicity-threshold"},
                description = "Variant with greater pathogenicity score is considered deleterious (default: ${DEFAULT-VALUE}).")
        public float pathogenicityThreshold = .8f;

        @CommandLine.Option(names = {"--default-allele-frequency"},
                description = "Variant with greater allele frequency in at least one population is considered common (default: ${DEFAULT-VALUE}).")
        public float defaultAlleleFrequency = 1E-5f;
    }

}
