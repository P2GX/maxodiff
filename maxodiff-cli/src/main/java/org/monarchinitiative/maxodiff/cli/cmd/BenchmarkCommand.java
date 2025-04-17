package org.monarchinitiative.maxodiff.cli.cmd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.monarchinitiative.lirical.configuration.impl.BundledBackgroundVariantFrequencyServiceFactory;
import org.monarchinitiative.lirical.core.Lirical;
import org.monarchinitiative.lirical.core.analysis.AnalysisOptions;
import org.monarchinitiative.lirical.core.analysis.probability.PretestDiseaseProbabilities;
import org.monarchinitiative.lirical.core.service.PhenotypeService;
import org.monarchinitiative.lirical.io.analysis.PhenopacketData;
import org.monarchinitiative.maxodiff.config.MaxodiffDataResolver;
import org.monarchinitiative.maxodiff.config.MaxodiffPropsConfiguration;
import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.*;
import org.monarchinitiative.maxodiff.core.analysis.refinement.*;
import org.monarchinitiative.maxodiff.core.model.*;
import org.monarchinitiative.maxodiff.lirical.PhenopacketFileParser;
import org.monarchinitiative.maxodiff.lirical.*;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.io.MinimalOntologyLoader;
import org.monarchinitiative.phenol.io.OntologyLoader;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;


/**
 * Perform Differential Diagnosis calculations
 */

@CommandLine.Command(name = "benchmark", aliases = {"B"},
        mixinStandardHelpOptions = true,
        description = "benchmark maxodiff analysis")
public class BenchmarkCommand extends DifferentialDiagnosisCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DifferentialDiagnosisCommand.class);

    private static ObjectMapper OBJECT_MAPPER;

    @CommandLine.Option(names = {"-B", "--batchDir"},
            description = "Path to directory containing phenopackets.")
    protected String batchDir;

    @CommandLine.Option(names = {"-W", "--weights"},
            split=",",
            arity = "1..*",
            description = "Comma-separated list of weight values to use in final score calculation.")
    public List<Double> weightsArg;

    @CommandLine.Option(names = {"-N", "--nDiseasesList"},
//            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of posttest probability thresholds for filtering diseases to include in differential diagnosis.")
    protected List<Integer> nDiseasesArg;

    @CommandLine.Option(names = {"-NR", "--nRepetitionsList"},
//            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of numbers of repetitions for running differential diagnosis.")
    protected List<Integer> nRepetitionsArg;


    @CommandLine.Option(names = {"--dummy"},
            description = "Whether to do dummy refinement (default: ${DEFAULT-VALUE}).")
    protected boolean dummy = false;

    @CommandLine.Option(names = {"--removeSampleTerms"},
            description = "Whether to remove Sample HPO terms before analysis (default: ${DEFAULT-VALUE}).")
    protected boolean removeSampleTerms = false;

    @CommandLine.Option(names = {"-o", "--outputFilename"},
            description = "Filename of the benchmark results CSV file. The CSV is compressed if the path has the '.gz' suffix")
    protected Path outputName;

    @CommandLine.Option(names = {"--refinerTypes"},
//            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of differential diagnosis refiners.")
    protected List<String> refinerTypes;


    @CommandLine.Option(names = {"-R", "--removeIdsFile"},
            description = "Path to file containing term Ids to remove for each phenopacket.")
    protected String removeIdsFile;


    @Override
    public Integer execute() throws Exception {

        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        OBJECT_MAPPER.registerModule(new Jdk8Module());

        Ontology ontology = OntologyLoader.loadOntology(MaxodiffDataResolver.of(maxoDataPath).hpoJson().toFile());
        MinimalOntology minimalOntology = MinimalOntologyLoader.loadOntology(MaxodiffDataResolver.of(maxoDataPath).hpoJson().toFile());

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
        List<Integer> nRepetitionsList = new ArrayList<>();
        nRepetitionsArg.forEach(nRepetitionsList::add);
        List<String> refinersList = new ArrayList<>();
        if (refinerTypes != null)
            refinerTypes.forEach(refinersList::add);

        Lirical lirical = prepareLirical();
        PhenotypeService phenotypeService = lirical.phenotypeService();
        BundledBackgroundVariantFrequencyServiceFactory bundledBackgroundVariantFrequencyServiceFactory =
                BundledBackgroundVariantFrequencyServiceFactory.getInstance();
        Set<TermId> liricalDiseaseIds = lirical.phenotypeService().diseases().diseaseIds();

        try (MaxodiffLiricalAnalysisRunner maxodiffLiricalAnalysisRunner =
                     MaxodiffLiricalAnalysisRunnerImpl.of(phenotypeService, 4)) {
            LiricalDifferentialDiagnosisEngineConfigurer liricalDifferentialDiagnosisEngineConfigurer = LiricalDifferentialDiagnosisEngineConfigurer.of(maxodiffLiricalAnalysisRunner);
            var analysisOptions = AnalysisOptions.builder()
                    .useStrictPenalties(runConfiguration.strict)
                    .useGlobal(runConfiguration.globalAnalysisMode)
                    .pretestProbability(PretestDiseaseProbabilities.uniform(liricalDiseaseIds))
                    .build();
            LiricalDifferentialDiagnosisEngine engine = liricalDifferentialDiagnosisEngineConfigurer.configure(analysisOptions);

            // Make maxodiffRefiner
            MaxodiffDataResolver maxodiffDataResolver = MaxodiffDataResolver.of(maxoDataPath);
            MaxodiffPropsConfiguration maxodiffPropsConfiguration = MaxodiffPropsConfiguration.createConfig(maxodiffDataResolver);
            Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap = maxodiffPropsConfiguration.maxoAnnotsMap();

            Map<String, DiffDiagRefiner> refiners = new HashMap<>();
            refiners.put("MaxoDiff", maxodiffPropsConfiguration.diffDiagRefiner("score"));
            for (String refiner : refinersList) {
                refiners.put(refiner, maxodiffPropsConfiguration.diffDiagRefiner(refiner));
            }

            BiometadataService biometadataService = maxodiffPropsConfiguration.biometadataService();

            try (BufferedWriter writer = openWriter(outputName); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
                printer.printRecord("phenopacket", "all_sample_ids", "n_sample_ids", "n_diseases", "n_repetitions",
                        "maxo_id", "maxo_label", "maxo_final_score", "n_all_maxo_hpo_ids",
                        "top_maxo_hpo_ids", "n_top_maxo_hpo_ids", "mean_n_disc_phen", "diff",
                        "validation_rank", "validation_weightedRank",
                        "validation_score", "refiner_type"); // header


                Set<TermId> allMaxoTerms = MaxoHpoTermIdMaps.getMaxoToHpoTermIdMap(hpoToMaxoTermMap).keySet();
                Set<TermId> allMaxoAscertainablePhenotypes = new HashSet<>();
                long allMaxoAscertainablePhenoypesCalcTime = 0;
                //5302 phenotypes discoverable by 257 MAxO terms
                double nAllMaxoTerms = 257;
                int nAllMaxoDiscoverablePhenotypes = 5302;
                double meanNDiscoverablePhenotypesAllMaxoTerms = nAllMaxoTerms / nAllMaxoDiscoverablePhenotypes;
                int p = 1;
                int nPhenopackets = phenopacketPaths.size();
                for (int i = 0; i < nPhenopackets; i++) {
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
                        List<TermId> allSampleHpoTerms = Stream.of(sample.presentHpoTermIds(), sample.excludedHpoTermIds())
                                .flatMap(Collection::stream).toList();
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
//                                "removedTerms",
                                "lirical",
                                "results");
                        String ddOutputPath = String.join(File.separator, outputDir.toString(), outFilename + ".csv");
                        writeDifferentialDiagnosisResults(phenopacketName, differentialDiagnoses, Path.of(ddOutputPath));

                        //TODO? get list of diseases from LIRICAL results, and add diseases from CLI arg to total list for analysis

                        Map<Integer, Map<TermId, List<DifferentialDiagnosis>>> nDiseaseMaxoTermToDifferentialDiagnosesMap = new HashMap<>();
                        for (Map.Entry<String, DiffDiagRefiner> e : refiners.entrySet()) {
                            for (int nDiseases : nDiseasesList) {
                                MaxoHpoTermProbabilities maxoHpoTermProbabilities = null;
                                List<DifferentialDiagnosis> initialDiagnoses = List.of();
                                if (e.getValue() instanceof MaxoDiffRefiner) {
                                    HpoDiseases hpoDiseases = phenotypeService.diseases();
                                    initialDiagnoses = differentialDiagnoses.subList(0, nDiseases);

                                    DiseaseModelProbability diseaseModelProbability = null;
                                    switch (diseaseProbModel) {
                                        case "ranked" -> diseaseModelProbability = DiseaseModelProbability.ranked(initialDiagnoses);
                                        case "softmax" -> diseaseModelProbability = DiseaseModelProbability.softmax(initialDiagnoses);
                                        case "expDecay" -> diseaseModelProbability = DiseaseModelProbability.exponentialDecay(initialDiagnoses);
                                    }

                                    maxoHpoTermProbabilities = new MaxoHpoTermProbabilities(hpoDiseases,
                                            hpoToMaxoTermMap,
                                            initialDiagnoses,
                                            diseaseModelProbability);

                                    if (allMaxoAscertainablePhenotypes.isEmpty() && meanNDiscoverablePhenotypesAllMaxoTerms == 0) {
                                        LOGGER.info("Getting All Maxo Ascertainable Phenotypes...");
                                        MaxoHpoTermProbabilities allMaxoHpoTermProbabilities = new MaxoHpoTermProbabilities(hpoDiseases,
                                                hpoToMaxoTermMap,
                                                differentialDiagnoses,
                                                diseaseModelProbability);

                                        List<DifferentialDiagnosis> orderedDiagnoses = differentialDiagnoses.stream()
                                                .sorted(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed())
                                                .toList();
                                        List<HpoDisease> diseases = e.getValue().getDiseases(orderedDiagnoses);
                                        Map<TermId, List<HpoFrequency>> hpoTermCounts = e.getValue().getHpoTermCounts(diseases);
                                        Map<TermId, Set<TermId>> fullMaxoToHpoTermIdMap = e.getValue().getMaxoToHpoTermIdMap(List.of(), hpoTermCounts);

                                        int m = 1;
                                        int nMaxoTerms = allMaxoTerms.size();
                                        long start = System.currentTimeMillis();
                                        for (TermId maxoId : allMaxoTerms) {
                                            LOGGER.info(maxoId.toString());
                                            Set<TermId> maxoAscertainablePhenotypes = allMaxoHpoTermProbabilities.getDiscoverableByMaxoHpoTerms(sample, maxoId, fullMaxoToHpoTermIdMap);
                                            allMaxoAscertainablePhenotypes.addAll(maxoAscertainablePhenotypes);
                                            float percent = (((float) m) / nMaxoTerms) * 100;
                                            LOGGER.info("Finished {} of {} MAxO terms. {}% complete.", m, nMaxoTerms, percent);
                                            m++;
                                        }
                                        long end = System.currentTimeMillis();
                                        allMaxoAscertainablePhenoypesCalcTime = (end - start) / 1000;

                                        meanNDiscoverablePhenotypesAllMaxoTerms = (double) allMaxoAscertainablePhenotypes.size() / allMaxoTerms.size();

                                        Path allMaxoAscPhenPath = getAllMaxoAscPhenPath("allMaxoAscPhenotypes");

                                        writeAllMaxoAscPhenotypes(allMaxoAscertainablePhenotypes, allMaxoAscPhenPath);
                                    }
                                }

                                Map<Double, Integer> weightToTopMaxoNAscertainablePhenotypesMap = new HashMap<>();
                                for (double weight : weights) {
                                    for (int nRepetitions : nRepetitionsList) {
                                        RefinementOptions options = RefinementOptions.of(nDiseases, nRepetitions, weight);
                                        LOGGER.info("{}: {}", e.getKey(), e.getValue());
                                        LOGGER.info("n Diseases = {}, n Repetitions = {}, Weight = {}", nDiseases, nRepetitions, weight);
                                        List<DifferentialDiagnosis> orderedDiagnoses = e.getValue().getOrderedDiagnoses(differentialDiagnoses, options);
                                        List<HpoDisease> diseases = e.getValue().getDiseases(orderedDiagnoses);
                                        Map<TermId, List<HpoFrequency>> hpoTermCounts = e.getValue().getHpoTermCounts(diseases);
                                        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = e.getValue().getMaxoToHpoTermIdMap(termIdsToRemove, hpoTermCounts);

                                        Map<TermId, List<DifferentialDiagnosis>> maxoTermToDifferentialDiagnosesMap = null;
                                        if (!(e.getValue() instanceof MaxoDiffRefiner) & !(e.getValue() instanceof DummyDiffDiagRefiner)) {
                                            maxoTermToDifferentialDiagnosesMap = getMaxoTermDifferentialDiagnosesMap(
                                                    e.getValue(), options, sample, engine, nDiseaseMaxoTermToDifferentialDiagnosesMap, maxoToHpoTermIdMap);
                                        }

                                        Set<TermId> initialDiagnosesIds = Set.of();
                                        RefinementResults refinementResults;
                                        if (e.getValue() instanceof MaxoDiffRefiner) {

                                            initialDiagnosesIds = initialDiagnoses.stream()
                                                    .map(DifferentialDiagnosis::diseaseId)
                                                    .collect(Collectors.toSet());

                                            var diseaseSubsetOptions = AnalysisOptions.builder()
                                                    //                    .setDiseaseDatabases(List.of(DiseaseDatabase.OMIM))
                                                    .useStrictPenalties(runConfiguration.strict)
                                                    .useGlobal(runConfiguration.globalAnalysisMode)
                                                    .pretestProbability(PretestDiseaseProbabilities.uniform(initialDiagnosesIds))
                                                    .addTargetDiseases(initialDiagnosesIds)
                                                    //                .includeDiseasesWithNoDeleteriousVariants(true)
                                                    .build();
                                            LiricalDifferentialDiagnosisEngine diseaseSubsetEngine = liricalDifferentialDiagnosisEngineConfigurer.configure(diseaseSubsetOptions);

                                            RankMaxo rankMaxo = new RankMaxo(hpoToMaxoTermMap, maxoToHpoTermIdMap, maxoHpoTermProbabilities, diseaseSubsetEngine,
                                                    minimalOntology, ontology);

                                            refinementResults = e.getValue().run(sample,
                                                    orderedDiagnoses,
                                                    options,
                                                    rankMaxo,
                                                    hpoTermCounts,
                                                    maxoToHpoTermIdMap);
                                        } else {
                                            // Get List of Refinement results: maxo term scores and frequencies
                                            refinementResults = e.getValue().run(sample, orderedDiagnoses, options, engine,
                                                    maxoToHpoTermIdMap, hpoTermCounts, maxoTermToDifferentialDiagnosesMap);
                                        }

                                        List<MaxodiffResult> resultsList = new ArrayList<>(refinementResults.maxodiffResults().stream().toList());
                                        if (e.getValue() instanceof MaxoDiffKolmogorovSmirnovRefiner) {
                                            resultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.maxoTermScore().scoreDiff()));
                                        } else if (e.getValue() instanceof MaxoDiffRefiner) {
                                            resultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.rankMaxoScore().maxoScore()).reversed());
                                        }else {
                                            resultsList.sort(Comparator.<MaxodiffResult>comparingDouble(mr -> mr.maxoTermScore().scoreDiff()).reversed());
                                        }
                                        String fileName = String.join("_",
                                                phenopacketName.replace(".json", ""),
                                                "n" + nDiseases,
                                                "nr" + nRepetitions,
                                                e.getKey() + ".json");
                                        Path maxodiffResultsFilePath = Path.of(String.join(File.separator, outputDir.toString(), fileName));
                                        writeToJsonFile(maxodiffResultsFilePath, refinementResults);

                                        // Test new validation procedure
                                        if (e.getValue() instanceof MaxoDiffRefiner) {
                                            assert maxoHpoTermProbabilities != null;
                                            CandidateDiseaseScores candidateDiseaseScores = new CandidateDiseaseScores(maxoHpoTermProbabilities, minimalOntology, ontology);
                                            // Get highest score MAxO term id
                                            MaxodiffResult topResult = resultsList.getFirst();
                                            TermId topMaxoId = topResult.rankMaxoScore().maxoId();

                                            String maxScoreTermLabel = biometadataService.maxoLabel(topMaxoId.toString()).orElse("unknown");
                                            double maxScoreValue = topResult.rankMaxoScore().maxoScore(); //maxoTermScore().scoreDiff();

                                            LOGGER.info("{}: n Diseases = {}, n Repetitions = {}, Weight = {}", e.getKey(), nDiseases, nRepetitions, weight);

                                            LOGGER.info("Max Score: {} ({}) = {}", topMaxoId, maxScoreTermLabel, maxScoreValue);

//                                            MaxoDDResults maxoDDResults = candidateDiseaseScores
//                                                    .getScoresForMaxoTerm(sample, topMaxoId, engine, initialDiagnosesIds, hpoToMaxoTermMap);

//                                            List<DifferentialDiagnosis> maxoTermDiagnoses = maxoDDResults.maxoDifferentialDiagnoses();
                                            LOGGER.info("Getting Top Maxo Ascertainable Phenotypes...");
                                            Set<TermId> topMaxoAscertainablePhenotypes = topResult.rankMaxoScore().discoverableObservedHpoTermIds();//maxoHpoTermProbabilities.getDiscoverableByMaxoHpoTerms(sample, topMaxoId, maxoToHpoTermIdMap);

                                            double diff = topMaxoAscertainablePhenotypes.size() - meanNDiscoverablePhenotypesAllMaxoTerms;

//                                            double validationScore_rank = ValidationModel.rankDiff(initialDiagnoses, maxoTermDiagnoses).validationScore();
//                                            double validationScore_weightedRank = ValidationModel.weightedRankDiff(initialDiagnoses, maxoTermDiagnoses).validationScore();
//                                            double validationScore_score = ValidationModel.scoreDiff(initialDiagnoses, maxoTermDiagnoses).validationScore();

                                            writeResults(phenopacketName, allSampleHpoTerms, allSampleHpoTerms.size(), nDiseases, nRepetitions,
                                                    topMaxoId.toString(), maxScoreTermLabel, maxScoreValue, nAllMaxoDiscoverablePhenotypes,
                                                    topMaxoAscertainablePhenotypes, topMaxoAscertainablePhenotypes.size(),
                                                    meanNDiscoverablePhenotypesAllMaxoTerms, diff,
//                                                    validationScore_rank, validationScore_weightedRank, validationScore_score,
                                                    e.getKey(), printer);

                                        }

                                        if (e.getKey().equals("rank") | e.getKey().equals("ddScore") | e.getKey().equals("ksTest")) {
                                            break;
                                        }
                                    }
                                }

                                if (e.getKey().equals("ksTest")) {
                                    break;
                                }
                            }
                        }
                        float percent = (((float) p) / nPhenopackets) * 100;
                        LOGGER.info("Finished benchmark for {} ({} of {}. {}% complete)", phenopacketName, p, nPhenopackets, percent);
                        p++;
                    } catch (Exception ex) {
                        LOGGER.info(ex.getMessage());
                    }
                }
//                long HH =  allMaxoAscertainablePhenoypesCalcTime / 3600;
//                long MM = (allMaxoAscertainablePhenoypesCalcTime % 3600) / 60;
//                long SS = allMaxoAscertainablePhenoypesCalcTime % 60;
//                String timeInHHMMSS = String.format("%02d:%02d:%02d", 2, 44, 40);
//                LOGGER.info("All MAxO Ascertainable Phenotypes Calculated in " + timeInHHMMSS);

            }
            LOGGER.info("Finished benchmark.");
        }

        return 0;
    }

    private Path getAllMaxoAscPhenPath(String pathNameAddition) {
        String outputFileNameStr = outputName.getFileName().toString();
        String allMaxoAscPhenFileName = outputFileNameStr.replace(".csv", "_" + pathNameAddition + ".csv");
        String outPath = outputName.toAbsolutePath().toString();
        String allMaxoAscPhenPathStr = outPath.replace(outputFileNameStr, allMaxoAscPhenFileName);
        return Path.of(allMaxoAscPhenPathStr);
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
                                     List<TermId> sampleIds,
                                     int nSampleIds,
                                     int nDiseases,
                                     double weight,
                                     String maxoId,
                                     String maxoLabel,
                                     double maxoFinalScore,
                                     int nAllMaxoHpoTerms,
                                     Set<TermId> topMaxoHpoTerms,
                                     int nTopMaxoHpoTerms,
                                     double meanNDiscPhenotypes,
                                     double diff,
//                                     double validationScore_rank,
//                                     double validationScore_weightedRank,
//                                     double validationScore_score,
                                     String refinerType,
                                     CSVPrinter printer) {

        try {
            printer.print(phenopacketName);
            printer.print(sampleIds);
            printer.print(nSampleIds);
            printer.print(nDiseases);
            printer.print(weight);
            printer.print(maxoId);
            printer.print(maxoLabel);
            printer.print(maxoFinalScore);
            printer.print(nAllMaxoHpoTerms);
            printer.print(topMaxoHpoTerms);
            printer.print(nTopMaxoHpoTerms);
            printer.print(meanNDiscPhenotypes);
            printer.print(diff);
//            printer.print(validationScore_rank);
//            printer.print(validationScore_weightedRank);
//            printer.print(validationScore_score);
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

    /**
     * Write results of a differential diagnosis into the provided {@code printer}.
     */
    private static void writeAllMaxoAscPhenotypes(Set<TermId> allMaxoAscPhenotypes, Path outputName) {

        try (BufferedWriter writer = openWriter(outputName); CSVPrinter printer = CSVFormat.DEFAULT.print(writer)) {
            for (TermId hpoId : allMaxoAscPhenotypes) {
                printer.print(hpoId);
                printer.println();
            }
        } catch (IOException e) {
            LOGGER.error("Error writing all MAxO ascertainable phenotype results: {}", e.getMessage(), e);
        }
    }

    protected Map<TermId, List<DifferentialDiagnosis>> getMaxoTermDifferentialDiagnosesMap(DiffDiagRefiner refiner, RefinementOptions options,
                                                                                           Sample sample, LiricalDifferentialDiagnosisEngine engine,
                                                                                           Map<Integer, Map<TermId, List<DifferentialDiagnosis>>> nDiseaseMaxoTermToDifferentialDiagnosesMap,
                                                                                           Map<TermId, Set<TermId>> maxoToHpoTermIdMap) {
        Map<TermId, List<DifferentialDiagnosis>> maxoTermToDifferentialDiagnosesMap = null;
        if (refiner instanceof MaxoDiffDDScoreRefiner | refiner instanceof MaxoDiffRankRefiner |
                refiner instanceof MaxoDiffRefiner | refiner instanceof DummyDiffDiagRefiner) {
            Integer nMapDiseases = options.nDiseases();
            if (!nDiseaseMaxoTermToDifferentialDiagnosesMap.containsKey(nMapDiseases)) {
                maxoTermToDifferentialDiagnosesMap = refiner
                        .getMaxoTermToDifferentialDiagnosesMap(sample, engine, maxoToHpoTermIdMap, nMapDiseases);
                nDiseaseMaxoTermToDifferentialDiagnosesMap.put(nMapDiseases, maxoTermToDifferentialDiagnosesMap);
            }
            maxoTermToDifferentialDiagnosesMap = nDiseaseMaxoTermToDifferentialDiagnosesMap.get(nMapDiseases);
        } else if (refiner instanceof MaxoDiffKolmogorovSmirnovRefiner) {
            Integer nMapDiseases = 100;
            maxoTermToDifferentialDiagnosesMap = refiner
                    .getMaxoTermToDifferentialDiagnosesMap(sample, engine, maxoToHpoTermIdMap, nMapDiseases);
        }

        return maxoTermToDifferentialDiagnosesMap;
    }

}
