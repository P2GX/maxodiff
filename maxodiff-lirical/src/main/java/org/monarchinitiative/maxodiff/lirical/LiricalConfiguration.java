package org.monarchinitiative.maxodiff.lirical;

import org.monarchinitiative.lirical.configuration.LiricalBuilder;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class LiricalConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalDifferentialDiagnosisEngine.class);
    private static final Properties PROPERTIES = readProperties();
    protected static final String LIRICAL_VERSION = PROPERTIES.getProperty("lirical.version", "unknown version");

    Path liricalDataDirectory;
    Path exomiserDatabase;
    String genomeBuild;
    TranscriptDatabase transcriptDb;
    Float pathogenicityThreshold;
    Double defaultVariantBackgroundFrequency;
    boolean strict;
    boolean globalAnalysisMode;

    Lirical lirical;


    public static LiricalConfiguration of(Path liricalDataDirectory, Path exomiserDatabase, String genomeBuild,
                                        TranscriptDatabase transcriptDatabase, Float pathogenicityThreshold,
                                        Double defaultVariantBackgroundFrequency, boolean strict, boolean globalAnalysisMode) throws LiricalException {

        return new LiricalConfiguration(liricalDataDirectory, exomiserDatabase, genomeBuild,
                transcriptDatabase, pathogenicityThreshold, defaultVariantBackgroundFrequency,
                strict, globalAnalysisMode);
    }

    public static LiricalConfiguration of(LiricalRecord liricalRecord) throws LiricalException {

        return new LiricalConfiguration(liricalRecord.liricalDataDir(), liricalRecord.exomiserPath(),
                liricalRecord.genomeBuild(), liricalRecord.transcriptDatabase(), liricalRecord.pathogenicityThreshold(),
                liricalRecord.defaultVariantBackgroundFrequency(), liricalRecord.strict(), liricalRecord.globalAnalysisMode());
    }

    private LiricalConfiguration(Path liricalDataDirectory, Path exomiserDatabase, String genomeBuild,
                                TranscriptDatabase transcriptDatabase, Float pathogenicityThreshold,
                                Double defaultVariantBackgroundFrequency, boolean strict, boolean globalAnalysisMode) throws LiricalException {

        this.liricalDataDirectory = liricalDataDirectory;
        this.exomiserDatabase = exomiserDatabase;
        this.genomeBuild = genomeBuild;
        this.transcriptDb = transcriptDatabase;
        this.pathogenicityThreshold = pathogenicityThreshold;
        this.defaultVariantBackgroundFrequency = defaultVariantBackgroundFrequency;
        this.strict = strict;
        this.globalAnalysisMode = globalAnalysisMode;

        this.lirical = prepareLirical();
    }

    public Lirical lirical() {
        return lirical;
    }

    protected Lirical prepareLirical() throws LiricalException {
        // Check input.
        List<String> errors = checkInput();
        if (!errors.isEmpty())
            throw new LiricalException(String.format("Errors: %s", String.join(", ", errors)));

        // Bootstrap LIRICAL.
        return bootstrapLirical();
    }

    protected List<String> checkInput() {
        List<String> errors = new LinkedList<>();
        // resources
        LOGGER.info(String.valueOf(liricalDataDirectory));
        if (liricalDataDirectory == null) {
            String msg = "Path to Lirical data directory must be provided via `-d | --data` option";
            LOGGER.error(msg);
            errors.add(msg);
        }
        return errors;
    }

    protected Lirical bootstrapLirical() throws LiricalDataException {
        LOGGER.info("Spooling up Lirical v{}", LIRICAL_VERSION);
        if (exomiserDatabase == null) {
            return LiricalBuilder.builder(liricalDataDirectory)
                    // .exomiserVariantDbPath(parseGenomeBuild(getGenomeBuild()), dataSection.exomiserDatabase)
//                .defaultVariantAlleleFrequency(runConfiguration.defaultAlleleFrequency)
                    .build();
        }


        return LiricalBuilder.builder(liricalDataDirectory)
                .exomiserVariantDbPath(parseGenomeBuild(genomeBuild), exomiserDatabase)
//                .defaultVariantAlleleFrequency(runConfiguration.defaultAlleleFrequency)
                .build();
    }

    private static Properties readProperties() {
        Properties properties = new Properties();

        try (InputStream is = LiricalDifferentialDiagnosisEngine.class.getResourceAsStream("/lirical.properties")) {
            properties.load(is);
        } catch (IOException e) {
            LOGGER.warn("Error loading properties: {}", e.getMessage());
        }
        return properties;
    }

    public AnalysisOptions prepareAnalysisOptions() throws LiricalDataException {
        Map<TermId, Double> diseaseIdToPretestProba = new HashMap<>();
        Set<TermId> diseaseIds = lirical.phenotypeService().diseases().diseaseIds();
        int nTotalDiseases = diseaseIds.size();
        diseaseIds.stream().forEach(id -> diseaseIdToPretestProba.put(id, 1./nTotalDiseases));
        PretestDiseaseProbability pretestProba = PretestDiseaseProbability.of(diseaseIdToPretestProba);

        return AnalysisOptions.builder()
                .genomeBuild(parseGenomeBuild(genomeBuild))
                .transcriptDatabase(transcriptDb)
                .setDiseaseDatabases(List.of(DiseaseDatabase.OMIM))
                .variantDeleteriousnessThreshold(pathogenicityThreshold)
                .defaultVariantBackgroundFrequency(defaultVariantBackgroundFrequency)
                .useStrictPenalties(strict)
                .useGlobal(globalAnalysisMode)
                .pretestProbability(pretestProba)
                .includeDiseasesWithNoDeleteriousVariants(true)
                .build();
    }

    protected GenomeBuild parseGenomeBuild(String genomeBuild) throws LiricalDataException {
        Optional<GenomeBuild> genomeBuildOptional = GenomeBuild.parse(genomeBuild);
        if (genomeBuildOptional.isEmpty())
            throw new LiricalDataException("Unknown genome build: '" + genomeBuild + "'");
        return genomeBuildOptional.get();
    }
}
