package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.lirical.configuration.LiricalBuilder;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.*;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbability;
import org.monarchinitiative.lirical.core.exception.LiricalException;
import org.monarchinitiative.lirical.core.io.VariantParser;
import org.monarchinitiative.lirical.core.model.GenesAndGenotypes;
import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.Sex;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.monarchinitiative.lirical.io.LiricalDataException;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporter;
import org.monarchinitiative.lirical.io.analysis.PhenopacketImporters;
import org.monarchinitiative.phenol.annotations.io.hpo.DiseaseDatabase;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


public class LiricalAnalysis {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiricalAnalysis.class);
    private static final Properties PROPERTIES = readProperties();
    protected static final String LIRICAL_VERSION = PROPERTIES.getProperty("lirical.version", "unknown version");

    String genomeBuild;
    TranscriptDatabase transcriptDb;
    Float pathogenicityThreshold;
    Double defaultVariantBackgroundFrequency;
    boolean strict;
    boolean globalAnalysisMode;
    Path liricalDataDirectory;
    Path exomiserDatabase;
    Path vcfPath;

    public record LiricalRecord(String genomeBuild, TranscriptDatabase transcriptDatabase, Float pathogenicityThreshold,
                                Double defaultVariantBackgroundFrequency, boolean strict, boolean globalAnalysisMode,
                                Path liricalDataDir, Path exomiserPath, Path vcfPath) {}

    public LiricalAnalysis(String genomeBuild, TranscriptDatabase transcriptDatabase, Float pathogenicityThreshold,
                           Double defaultVariantBackgroundFrequency, boolean strict, boolean globalAnalysisMode,
                           Path liricalDataDirectory, Path exomiserDatabase, Path vcfPath) {
        this.genomeBuild = genomeBuild;
        this.transcriptDb = transcriptDatabase;
        this.pathogenicityThreshold = pathogenicityThreshold;
        this.defaultVariantBackgroundFrequency = defaultVariantBackgroundFrequency;
        this.strict = strict;
        this.globalAnalysisMode = globalAnalysisMode;
        this.liricalDataDirectory = liricalDataDirectory;
        this.exomiserDatabase = exomiserDatabase;
        this.vcfPath = vcfPath;
    }

    public LiricalAnalysis(LiricalRecord liricalRecord) {
        this.genomeBuild = liricalRecord.genomeBuild();
        this.transcriptDb = liricalRecord.transcriptDatabase();
        this.pathogenicityThreshold = liricalRecord.pathogenicityThreshold();
        this.defaultVariantBackgroundFrequency = liricalRecord.defaultVariantBackgroundFrequency();
        this.strict = liricalRecord.strict();
        this.globalAnalysisMode = liricalRecord.globalAnalysisMode();
        this.liricalDataDirectory = liricalRecord.liricalDataDir();
        this.exomiserDatabase = liricalRecord.exomiserPath();
        this.vcfPath = liricalRecord.vcfPath();
    }

    public Path getLiricalDataDirectory() {
        return liricalDataDirectory;
    }

    private static Properties readProperties() {
        Properties properties = new Properties();

        try (InputStream is = LiricalAnalysis.class.getResourceAsStream("/lirical.properties")) {
            properties.load(is);
        } catch (IOException e) {
            LOGGER.warn("Error loading properties: {}", e.getMessage());
        }
        return properties;
    }

    protected List<String> checkInput() {
        List<String> errors = new LinkedList<>();
        // resources
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

    protected Lirical prepareLirical() throws IOException, LiricalException {
        // Check input.
        List<String> errors = checkInput();
        if (!errors.isEmpty())
            throw new LiricalException(String.format("Errors: %s", String.join(", ", errors)));

        // Bootstrap LIRICAL.
        Lirical lirical = bootstrapLirical();
        return lirical;
    }

    protected static PhenopacketData readPhenopacketData(Path phenopacketPath) throws LiricalParseException {
        PhenopacketData data = null;
        try (InputStream is = new BufferedInputStream(Files.newInputStream(phenopacketPath))) {
            PhenopacketImporter v2 = PhenopacketImporters.v2();
            data = v2.read(is);
            LOGGER.debug("Success!");
        } catch (Exception e) {
            LOGGER.debug("Unable to parse as v2 phenopacket, trying v1.");
        }

        if (data == null) {
            try (InputStream is = new BufferedInputStream(Files.newInputStream(phenopacketPath))) {
                PhenopacketImporter v1 = PhenopacketImporters.v1();
                data = v1.read(is);
                LOGGER.debug("Success!");
            } catch (IOException e) {
                LOGGER.debug("Unable to parser as v1 phenopacket.");
                throw new LiricalParseException("Unable to parse phenopacket from " + phenopacketPath.toAbsolutePath());
            }
        }

        // Check we have exactly one disease ID.
        if (data.diseaseIds().isEmpty())
            throw new LiricalParseException("Missing disease ID which is required for the benchmark!");
        else if (data.diseaseIds().size() > 1)
            throw new LiricalParseException("Saw >1 disease IDs {}, but we need exactly one for the benchmark!");
        return data;
    }

    protected GenomeBuild parseGenomeBuild(String genomeBuild) throws LiricalDataException {
        Optional<GenomeBuild> genomeBuildOptional = GenomeBuild.parse(genomeBuild);
        if (genomeBuildOptional.isEmpty())
            throw new LiricalDataException("Unknown genome build: '" + genomeBuild + "'");
        return genomeBuildOptional.get();
    }

    protected AnalysisOptions prepareAnalysisOptions(Lirical lirical) throws LiricalDataException {
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

    protected GenesAndGenotypes readVariants(Path vcfPath, Lirical lirical, GenomeBuild genomeBuild) throws Exception {
        if (vcfPath != null && Files.isRegularFile(vcfPath)) {
            Optional<VariantParser> variantParser = lirical.variantParserFactory()
                    .forPath(vcfPath, genomeBuild, transcriptDb);
            if (variantParser.isPresent()) {
                try (VariantParser parser = variantParser.get()) {
                    return GenesAndGenotypes.fromVariants(parser.sampleNames(), parser);
                }
            }
        }
        return GenesAndGenotypes.empty();
    }

    public AnalysisResults runLiricalAnalysis(Path phenopacketPath) throws Exception {
        // Prepare LIRICAL analysis options
        Lirical lirical = prepareLirical();
        AnalysisOptions analysisOptions = prepareAnalysisOptions(lirical);

        // Read variants if present.
        GenesAndGenotypes gene2Genotypes = readVariants(vcfPath, lirical, analysisOptions.genomeBuild());

        // Prepare LIRICAL analysis data.
        PhenopacketData phenopacketData = readPhenopacketData(phenopacketPath);
        AnalysisData analysisData = AnalysisData.of(phenopacketData.sampleId(),
                phenopacketData.parseAge().orElse(null),
                phenopacketData.parseSex().orElse(Sex.UNKNOWN),
                phenopacketData.presentHpoTermIds().toList(),
                phenopacketData.excludedHpoTermIds().toList(),
                gene2Genotypes);

        // Run the LIRICAL analysis.
        LOGGER.info("Starting the analysis: {}", analysisOptions);
        AnalysisResults results;
        try (LiricalAnalysisRunner analysisRunner = lirical.analysisRunner()) {
            results = analysisRunner.run(analysisData, analysisOptions);
        }

        return results;
    }
}
