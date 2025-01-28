package org.monarchinitiative.maxodiff.cli.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.lirical.configuration.impl.BundledBackgroundVariantFrequencyServiceFactory;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.config.MaxodiffPropsConfiguration;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.analysis.refinement.*;
import org.monarchinitiative.maxodiff.core.io.PhenopacketFileParser;
import org.monarchinitiative.maxodiff.core.lirical.*;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;


/**
 * Perform Differential Diagnosis calculations
 */

@CommandLine.Command(name = "newBenchmark", aliases = {"nB"},
        mixinStandardHelpOptions = true,
        description = "benchmark maxodiff analysis")
public class NewBenchmarkCommand extends BenchmarkCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DifferentialDiagnosisCommand.class);

    private static ObjectMapper OBJECT_MAPPER;

    @CommandLine.Option(names = {"-R", "--removeIdsFile"},
            description = "Path to file containing term Ids to remove for each phenopacket.")
    protected String removeIdsFile;


    @Override
    public Integer execute() throws Exception {

        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        OBJECT_MAPPER.registerModule(new Jdk8Module());

        List<Path> phenopacketPaths = new ArrayList<>();
        if (batchDir != null) {
            File folder = new File(batchDir);
            File[] files = folder.listFiles();
            assert files != null;
            for (File file : files) {
                BasicFileAttributes basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                if (basicFileAttributes.isRegularFile() && !basicFileAttributes.isDirectory() && !file.getName().startsWith(".")) {
                    phenopacketPaths.add(file.toPath());
                }
            }
        } else if (phenopacketPath != null) {
            phenopacketPaths.add(phenopacketPath);
        }
        Collections.sort(phenopacketPaths);

        List<Double> weights = new ArrayList<>();
        weightsArg.forEach(weights::add);
        List<Integer> nDiseasesList = new ArrayList<>();
        nDiseasesArg.forEach(nDiseasesList::add);
        List<String> refinersList = new ArrayList<>();
        if (refinerTypes != null)
            refinerTypes.forEach(refinersList::add);

        LiricalConfiguration liricalConfiguration = configureLirical();
        Lirical lirical = liricalConfiguration.lirical();
        PhenotypeService phenotypeService = lirical.phenotypeService();
        BundledBackgroundVariantFrequencyServiceFactory bundledBackgroundVariantFrequencyServiceFactory =
                BundledBackgroundVariantFrequencyServiceFactory.getInstance();
        Set<TermId> liricalDiseaseIds = lirical.phenotypeService().diseases().diseaseIds();

        try (MaxodiffLiricalAnalysisRunner maxodiffLiricalAnalysisRunner =
                     MaxodiffLiricalAnalysisRunnerImpl.of(phenotypeService,
                             bundledBackgroundVariantFrequencyServiceFactory, 1)) {
            LiricalDifferentialDiagnosisEngineConfigurer liricalDifferentialDiagnosisEngineConfigurer = LiricalDifferentialDiagnosisEngineConfigurer.of(maxodiffLiricalAnalysisRunner);
            var analysisOptions = liricalConfiguration.prepareAnalysisOptions(liricalDiseaseIds);
            LiricalDifferentialDiagnosisEngine engine = liricalDifferentialDiagnosisEngineConfigurer.configure(analysisOptions);

            // Make maxodiffRefiner
            MaxodiffDataResolver maxodiffDataResolver = MaxodiffDataResolver.of(maxoDataPath);
            MaxodiffPropsConfiguration maxodiffPropsConfiguration = MaxodiffPropsConfiguration.createConfig(maxodiffDataResolver);

            Map<String, DiffDiagRefiner> refiners = new HashMap<>();
            refiners.put("MaxoDiff", maxodiffPropsConfiguration.diffDiagRefiner("score"));
            for (String refiner : refinersList) {
                refiners.put(refiner, maxodiffPropsConfiguration.diffDiagRefiner(refiner));
            }

            BiometadataService biometadataService = maxodiffPropsConfiguration.biometadataService();

            try (BufferedWriter writer = openWriter(outputName); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
                printer.printRecord("phenopacket", "removed_ids", "n_diseases", "weight",
                        "maxo_id", "maxo_label", "maxo_final_score", "changed_disease_id",
                        "orig_disease_rank", "maxo_disease_rank", "orig_disease_score", "maxo_disease_score",
                        "maxo_ascertained_removed_ids", "maxo_ascertained_all_ids", "refiner_type"); // header

                for (int i = 0; i < phenopacketPaths.size(); i++) {
                    try {

                        Path pPath = phenopacketPaths.get(i);
                        PhenopacketData phenopacketData = PhenopacketFileParser.readPhenopacketData(pPath);
                        Sample sample = Sample.of(phenopacketData.sampleId(),
                                phenopacketData.presentHpoTermIds().toList(),
                                phenopacketData.excludedHpoTermIds().toList());

                        LOGGER.info(String.valueOf(phenopacketPath));
                        LOGGER.info("weights = {}", weights);
                        LOGGER.info("nDiseases = {}", nDiseasesList);
                        LOGGER.info("refiners = {}", refinersList);
                        String phenopacketName = pPath.toFile().getName();
                        List<TermId> termIdsToRemove = new ArrayList<>();
                        List<TermId> includedIds = new ArrayList<>(phenopacketData.presentHpoTermIds().toList());
                        List<TermId> excludedIds = new ArrayList<>(phenopacketData.excludedHpoTermIds().toList());
                        if (removeIdsFile != null) {
                            termIdsToRemove = getTermIdsToRemove(phenopacketName, removeIdsFile);
                            termIdsToRemove.forEach(includedIds::remove);
                            termIdsToRemove.forEach(excludedIds::remove);
                            sample = Sample.of(phenopacketData.sampleId(), includedIds, excludedIds);
                        }
                        if (removeSampleTerms) {
                            termIdsToRemove = Stream.of(sample.presentHpoTermIds(), sample.excludedHpoTermIds())
                                    .flatMap(Collection::stream).toList();
                        }
                        LOGGER.info("{} removed Ids = {}", phenopacketName, termIdsToRemove);

                        // Get initial differential diagnoses from running LIRICAL
                        List<DifferentialDiagnosis> differentialDiagnoses = engine.run(sample);

                        // Summarize the LIRICAL results.
                        String outFilename = String.join("_",
                                phenopacketName.replace(".json", ""),
                                "initial",
                                "removedTerms",
                                "lirical",
                                "results");
                        String ddOutputPath = String.join(File.separator, outputDir.toString(), outFilename + ".csv");
                        writeDifferentialDiagnosisResults(phenopacketName, differentialDiagnoses, Path.of(ddOutputPath));

                        //TODO? get list of diseases from LIRICAL results, and add diseases from CLI arg to total list for analysis

                        Map<Integer, Map<TermId, List<DifferentialDiagnosis>>> nDiseaseMaxoTermToDifferentialDiagnosesMap = new HashMap<>();
                        for (Map.Entry<String, DiffDiagRefiner> e : refiners.entrySet()) {
                            HpoDiseases hpoaDiseases = e.getValue().getHPOADiseases();
                            for (int nDiseases : nDiseasesList) {
                                for (double weight : weights) {
                                    RefinementOptions options = RefinementOptions.of(nDiseases, weight);
                                    LOGGER.info("{}: {}", e.getKey(), e.getValue());
                                    LOGGER.info("n Diseases = {}, Weight = {}", nDiseases, weight);
                                    List<DifferentialDiagnosis> orderedDiagnoses = e.getValue().getOrderedDiagnoses(differentialDiagnoses, options);
                                    List<HpoDisease> diseases = e.getValue().getDiseases(orderedDiagnoses);
                                    Map<TermId, List<HpoFrequency>> hpoTermCounts = e.getValue().getHpoTermCounts(diseases);
                                    Map<TermId, Set<TermId>> maxoToHpoTermIdMap = e.getValue().getMaxoToHpoTermIdMap(termIdsToRemove, hpoTermCounts);
                                    Map<TermId, Set<TermId>> fullMaxoToHpoTermIdMap = e.getValue().getMaxoToHpoTermIdMap(List.of(), hpoTermCounts);
                                    Map<TermId, List<DifferentialDiagnosis>> maxoTermToDifferentialDiagnosesMap = null;
                                    if (!(e.getValue() instanceof MaxoDiffRefiner) & !(e.getValue() instanceof DummyDiffDiagRefiner)) {
                                        maxoTermToDifferentialDiagnosesMap = getMaxoTermDifferentialDiagnosesMap(
                                                e.getValue(), options, sample, engine, nDiseaseMaxoTermToDifferentialDiagnosesMap, maxoToHpoTermIdMap);
                                    }
                                    // Get List of Refinement results: maxo term scores and frequencies
                                    RefinementResults refinementResults = e.getValue().run(sample, orderedDiagnoses, options, engine,
                                            maxoToHpoTermIdMap, hpoTermCounts, maxoTermToDifferentialDiagnosesMap);
                                    List<MaxodiffResult> resultsList = new ArrayList<>(refinementResults.maxodiffResults().stream().toList());
                                    if (e.getValue() instanceof MaxoDiffKolmogorovSmirnovRefiner) {
                                        resultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.maxoTermScore().scoreDiff()));
                                    } else {
                                        resultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.maxoTermScore().scoreDiff()).reversed());
                                    }
                                    String fileName = String.join("_",
                                            phenopacketName.replace(".json", ""),
                                            "n" + nDiseases,
                                            "w" + weight,
                                            e.getKey() + ".json");
                                    Path maxodiffResultsFilePath = Path.of(String.join(File.separator, outputDir.toString(), fileName));
                                    writeToJsonFile(maxodiffResultsFilePath, refinementResults);

                                    // Test new validation procedure
                                    // Calculate discoverable HPO terms
                                    Map<TermId, Integer> maxoTermDiscoverablePhenotypeSums = new HashMap<>();
                                    for (MaxodiffResult result : resultsList) {
                                        TermId maxoId = TermId.of(result.maxoTermScore().maxoId());
                                        int maxoTermDiscoverablePhenotypeSum = 0;
                                        for (DifferentialDiagnosis diagnosis : orderedDiagnoses) {
                                            TermId diseaseId = diagnosis.diseaseId();
                                            List<TermId> phenopacketDiscoverablePhenotypes = getPhenopacketDiscoverablePhenotypes(diseaseId,
                                                    hpoaDiseases, termIdsToRemove);
                                            LOGGER.info("pPackDiscPhenotypes = {}", phenopacketDiscoverablePhenotypes);
                                            List<TermId> maxoTermInferredExcludedPhenotypes = getMaxoTermInferredExcludedPhenotypes(phenopacketData,
                                                    hpoaDiseases, maxoToHpoTermIdMap.get(maxoId));
                                            LOGGER.info("maxoInferExclPhenotypes = {}", maxoTermInferredExcludedPhenotypes);
                                            Map<TermId, Set<TermId>> maxoTermDiscoverablePhenotypes = getMaxoTermDiscoverablePhenotypes(phenopacketDiscoverablePhenotypes,
                                                    maxoTermInferredExcludedPhenotypes, maxoId);
                                            LOGGER.info("pPack target disease: {}", phenopacketData.diseaseIds().get(0));
                                            LOGGER.info("diagnosis {}: {}", orderedDiagnoses.indexOf(diagnosis)+1, diseaseId);
                                            LOGGER.info("MAxO term {}: {} {}", resultsList.indexOf(result)+1, maxoId, biometadataService.maxoLabel(maxoId.toString()).orElse("unknown"));
                                            LOGGER.info("maxoDiscPhenotypes = {}", maxoTermDiscoverablePhenotypes);
                                            maxoTermDiscoverablePhenotypeSum += maxoTermDiscoverablePhenotypes.get(maxoId).size();
                                        }
                                        maxoTermDiscoverablePhenotypeSums.put(maxoId, maxoTermDiscoverablePhenotypeSum);
                                    }

                                    String maxoDiscoverableOutFilename = String.join("_",
                                            phenopacketName.replace(".json", ""),
                                            "n" + nDiseases,
                                            "w" + weight,
                                            e.getKey(),
                                            "discoverable",
                                            "phenotypes");
                                    String maxoDiscoverableOutputPath = String.join(File.separator, outputDir.toString(), maxoDiscoverableOutFilename + ".csv");
                                    writeDiscoverablePhenotypeResults(phenopacketName, maxoTermDiscoverablePhenotypeSums, Path.of(maxoDiscoverableOutputPath));

                                    // Old validation procedure
                                    // Take the MaXo term that has the highest score
                                    MaxodiffResult topResult = resultsList.get(0);
                                    String maxScoreMaxoTermId = topResult.maxoTermScore().maxoId();
                                    String maxScoreTermLabel = biometadataService.maxoLabel(maxScoreMaxoTermId).orElse("unknown");
                                    double maxScoreValue = topResult.maxoTermScore().scoreDiff();
                                    TermId changedDiseaseId = null;
                                    int origRank = 0;
                                    double origLR = 0;
                                    int maxoRank = 0;
                                    double maxoLR = 0;
                                    Set<TermId> removedIdsMaxoAscertained = new HashSet<>();
                                    Set<TermId> topMaxoHpoIds = fullMaxoToHpoTermIdMap.get(TermId.of(maxScoreMaxoTermId));

                                    if (removeIdsFile != null) {
                                        termIdsToRemove.stream().filter(topMaxoHpoIds::contains).forEach(includedIds::add);
                                        termIdsToRemove.stream().filter(topMaxoHpoIds::contains).forEach(excludedIds::add);
                                        termIdsToRemove.stream().filter(topMaxoHpoIds::contains).forEach(removedIdsMaxoAscertained::add);
                                        sample = Sample.of(phenopacketData.sampleId(), includedIds, excludedIds);
                                        List<DifferentialDiagnosis> differentialDiagnosesAddTermsBack = engine.run(sample);
                                        // Summarize the LIRICAL results.
                                        String addOutFilename = String.join("_",
                                                phenopacketName.replace(".json", ""),
                                                maxScoreMaxoTermId,
                                                "n" + nDiseases,
                                                "w" + weight,
                                                e.getKey(),
                                                "addTerms",
                                                "lirical",
                                                "results");
                                        String addDdOutputPath = String.join(File.separator, outputDir.toString(), addOutFilename + ".csv");
                                        writeDifferentialDiagnosisResults(phenopacketName, differentialDiagnosesAddTermsBack, Path.of(addDdOutputPath));

                                        changedDiseaseId = phenopacketData.diseaseIds().get(0); //phenopacket target disease
                                        TermId finalChangedDiseaseId1 = changedDiseaseId;
                                        List<DifferentialDiagnosis> changedDiseaseOrigDiagnosisList = differentialDiagnoses
                                                .stream().filter(dd -> dd.diseaseId().equals(finalChangedDiseaseId1)).toList();
                                        if (!changedDiseaseOrigDiagnosisList.isEmpty()) {
                                            DifferentialDiagnosis changedDiseaseOrigDiagnosis = changedDiseaseOrigDiagnosisList.get(0);
                                            origRank = differentialDiagnoses.indexOf(changedDiseaseOrigDiagnosis) + 1;
                                            origLR = changedDiseaseOrigDiagnosis.lr();
                                        }
                                        List<DifferentialDiagnosis> changedDiseaseAddBackDiagnosisList = differentialDiagnosesAddTermsBack
                                                .stream().filter(dd -> dd.diseaseId().equals(finalChangedDiseaseId1)).toList();
                                        if (!changedDiseaseAddBackDiagnosisList.isEmpty()) {
                                            DifferentialDiagnosis changedDiseaseAddBackDiagnosis = changedDiseaseAddBackDiagnosisList.get(0);
                                            maxoRank = differentialDiagnosesAddTermsBack.indexOf(changedDiseaseAddBackDiagnosis) + 1;
                                            maxoLR = changedDiseaseAddBackDiagnosis.lr();
                                            String outFilenameMaxo = String.join("_",
                                                    phenopacketName.replace(".json", ""),
                                                    maxScoreMaxoTermId,
                                                    "differential",
                                                    "diagnoses");
                                            Path outFilepathMaxo = Path.of(String.join(File.separator, outputDir.toString(), outFilenameMaxo + ".tsv"));
                                            LOGGER.info(outFilepathMaxo.toString());
                                            try (BufferedWriter writer2 = openWriter(outFilepathMaxo);
                                                 CSVPrinter printer2 = CSVFormat.DEFAULT.print(writer2)) {
                                                printer2.printRecord("disease_id", "score", "lr");
                                                writeDifferentialDiagnosesResultsFile(changedDiseaseAddBackDiagnosisList, printer2);
                                            } catch (Exception ex) {
                                                LOGGER.info(ex.getMessage());
                                            }
                                        }
                                    }

                                    LOGGER.info(e.getKey() + ": n Diseases = " + nDiseases + ", Weight = " + weight);

                                    LOGGER.info("Max Score: " + maxScoreMaxoTermId + " (" + maxScoreTermLabel + ")" + " = " + maxScoreValue);
                                    writeResults(phenopacketName, termIdsToRemove, nDiseases, weight,
                                            maxScoreMaxoTermId, maxScoreTermLabel, maxScoreValue, changedDiseaseId,
                                            origRank, maxoRank, origLR, maxoLR, removedIdsMaxoAscertained, topMaxoHpoIds,
                                            e.getKey(), printer);

                                    if (e.getKey().equals("rank") | e.getKey().equals("ddScore") | e.getKey().equals("ksTest")) {
                                        break;
                                    }
                                }

                                TermId diseaseId = phenopacketData.diseaseIds().get(0);

                                if (e.getKey().equals("ksTest")) {
                                    break;
                                }
                            }
                        }
                        LOGGER.info("Finished benchmark for " + phenopacketName);
                    } catch (Exception ex) {
                        LOGGER.info(ex.getMessage());
                    }
                }
            }
            LOGGER.info("Finished benchmark.");
        }

        return 0;
    }


    public void writeToJsonFile(Path filePath, RefinementResults results) throws IOException {
        ObjectWriter writer = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();
        writer.writeValue(new File(filePath.toString()), results);
    }

    public List<TermId> getTermIdsToRemove(String phenopacketName, String removeIdsMapFile) {
        List<TermId> termIdsToRemove = new ArrayList<>();
        File removeFile = new File(removeIdsMapFile);
        try (BufferedReader br = new BufferedReader(new FileReader(removeFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) continue;
                try {
                    String [] fields = line.split("\t");
                    String pName = fields[0];
                    if (pName.equals(phenopacketName)) {
                        termIdsToRemove = Arrays.stream(fields[1].split(",")).map(TermId::of).toList();
                    }
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        return termIdsToRemove;
    }

    private static BufferedWriter openWriter(Path outputPath) throws IOException {
        return outputPath.toFile().getName().endsWith(".gz")
                ? new BufferedWriter(new OutputStreamWriter(
                        new GZIPOutputStream(Files.newOutputStream(outputPath, StandardOpenOption.APPEND,
                                StandardOpenOption.CREATE, StandardOpenOption.WRITE))))
                : Files.newBufferedWriter(outputPath, StandardOpenOption.APPEND,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    /**
     * Write results of a single benchmark into the provided {@code printer}.
     */
    private static void writeResults(String phenopacketName,
                                     List<TermId> removedIds,
                                     int nDiseases,
                                     double weight,
                                     String maxoId,
                                     String maxoLabel,
                                     double maxoFinalScore,
                                     TermId changedDiseaseId,
                                     int origRank,
                                     int maxoRank,
                                     double origScore,
                                     double maxoScore,
                                     Set<TermId> removedIdsMaxoAscertained,
                                     Set<TermId> maxoAscertainedIds,
                                     String refinerType,
                                     CSVPrinter printer) {

        try {
            printer.print(phenopacketName);
            printer.print(removedIds);
            printer.print(nDiseases);
            printer.print(weight);
            printer.print(maxoId);
            printer.print(maxoLabel);
            printer.print(maxoFinalScore);
            printer.print(changedDiseaseId);
            printer.print(origRank);
            printer.print(maxoRank);
            printer.print(origScore);
            printer.print(maxoScore);
            printer.print(removedIdsMaxoAscertained);
            printer.print(maxoAscertainedIds);
            printer.print(refinerType);
            printer.println();
        } catch (IOException e) {
            LOGGER.error("Error writing results for {}: {}", phenopacketName, e.getMessage(), e);
        }
    }

    /**
     * Write results of a differential diagnosis into the provided {@code printer}.
     */
    private static void writeDifferentialDiagnosisResults(String phenopacketName,
                                     List<DifferentialDiagnosis> ddList,
                                     Path outputName) throws IOException {

        try (BufferedWriter writer = openWriter(outputName); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord("disease_id", "posttest_prob", "lr"); // header
            for (DifferentialDiagnosis dd : ddList) {
                printer.print(dd.diseaseId());
                printer.print(dd.score());
                printer.print(dd.lr());
                printer.println();
            }
        } catch (IOException e) {
            LOGGER.error("Error writing differential diagnosis results for {}: {}", phenopacketName, e.getMessage(), e);
        }
    }

    private static List<TermId> getPhenopacketDiscoverablePhenotypes(//PhenopacketData pData,
                                                                     TermId diseaseId,
                                                                     HpoDiseases hpoaDiseases,
                                                                     List<TermId> termIdsToRemove) {

        HpoDisease targetDisease;
        List<TermId> hpoIds = new ArrayList<>();
//        TermId targetDiseaseId = pData.diseaseIds().get(0);//should be diseaseId from loop of e.g. top 5?
        //System.out.println("getPpacketDiscPhenotypes disease Id = " + targetDiseaseId);
        Optional<HpoDisease> targetDiseaseOpt = hpoaDiseases.diseaseById(diseaseId);
        //System.out.println("getPpacketDiscPhenotypes targetDiseaseOpt = " + targetDiseaseOpt);
        if (targetDiseaseOpt.isPresent()) {
            targetDisease = targetDiseaseOpt.get();
            hpoIds = targetDisease.annotationTermIdList();
            //System.out.println("annotationTermIdList = " + hpoIds);
            System.out.println("termIdsToRemove = " + termIdsToRemove);
            hpoIds.removeAll(termIdsToRemove);
        }
        //System.out.println("pPacket Discoverable hpoIds = " + hpoIds);
        return hpoIds;
    }

    private static List<TermId> getMaxoTermInferredExcludedPhenotypes(PhenopacketData pData,
                                                                      HpoDiseases hpoaDiseases,
                                                                      Set<TermId> maxoTermAssociatedHpoIds) {

        List<TermId> phenopacketIdsMaxoAscertained = new ArrayList<>();
        pData.presentHpoTermIds().filter(maxoTermAssociatedHpoIds::contains).forEach(phenopacketIdsMaxoAscertained::add);
        pData.excludedHpoTermIds().filter(maxoTermAssociatedHpoIds::contains).forEach(phenopacketIdsMaxoAscertained::add);
        System.out.println("maxoTermAssociatedHpoIds = " + maxoTermAssociatedHpoIds);
        System.out.println("presentTermIds = " + pData.presentHpoTermIds().toList());
        System.out.println("presentTermIdsMaxoIntersect = " + pData.presentHpoTermIds()
                .filter(maxoTermAssociatedHpoIds::contains)
                .distinct()
                .toList());
        System.out.println("exclTermIds = " + pData.excludedHpoTermIds().toList());
        System.out.println("exclTermIdsMaxoIntersect = " + pData.excludedHpoTermIds()
                .filter(maxoTermAssociatedHpoIds::contains)
                .distinct()
                .toList());
        System.out.println("pPacketIdsMaxoAscertained = " + phenopacketIdsMaxoAscertained);
        List<TermId> inferredExcludedTerms = new ArrayList<>();
        if (!phenopacketIdsMaxoAscertained.isEmpty()) {
            hpoaDiseases.diseaseIds().stream().filter(maxoTermAssociatedHpoIds::contains).forEach(inferredExcludedTerms::add);
        }
        System.out.println("inferredExcludedTerms = " + inferredExcludedTerms);
        return inferredExcludedTerms;
    }

    private static Map<TermId, Set<TermId>> getMaxoTermDiscoverablePhenotypes(List<TermId> phenopacketDiscoverablePhenotypes,
                                                                                List<TermId> inferredExcludedTerms,
                                                                                TermId maxoId) {

        Set<TermId> maxoTermDiscoverableIds = new HashSet<>(phenopacketDiscoverablePhenotypes);
        inferredExcludedTerms.forEach(maxoTermDiscoverableIds::remove);
        Map<TermId, Set<TermId>> maxoTermDiscoverableIdsMap = new HashMap<>();
        maxoTermDiscoverableIdsMap.put(maxoId, maxoTermDiscoverableIds);
        System.out.println("maxoTermDiscoverableIdsMap = " + maxoTermDiscoverableIdsMap);
        return maxoTermDiscoverableIdsMap;
    }

    /**
     * Write results of a discoverable phenotype calculation into the provided {@code printer}.
     */
    private static void writeDiscoverablePhenotypeResults(String phenopacketName,
                                                          Map<TermId, Integer> maxoDiscoverablePhenotypeSums,
                                                          Path outputName) throws IOException {

        try (BufferedWriter writer = openWriter(outputName); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            printer.printRecord("maxo_id", "discoverable_phenotype_sum"); // header
            for (Map.Entry<TermId, Integer> entry : maxoDiscoverablePhenotypeSums.entrySet()) {
                TermId maxoId = entry.getKey();
                int sumDiscoverable = entry.getValue();
                printer.print(maxoId);
                printer.print(sumDiscoverable);
                printer.println();
            }
        } catch (IOException e) {
            LOGGER.error("Error writing discoverable phenotype results for {}: {}", phenopacketName, e.getMessage(), e);
        }
    }

}
