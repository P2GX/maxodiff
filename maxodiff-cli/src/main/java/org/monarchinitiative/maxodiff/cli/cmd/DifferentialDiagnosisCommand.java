package org.monarchinitiative.maxodiff.cli.cmd;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.analysis.LiricalResultsFileRecord;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermScoreMap;
import org.monarchinitiative.maxodiff.core.io.LiricalResultsFileParser;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;


/**
 * Perform Differential Diagnosis calculations
 */

@CommandLine.Command(name = "diagnosis", aliases = {"d"},
        mixinStandardHelpOptions = true,
        description = "maxodiff analysis")
public class DifferentialDiagnosisCommand implements Callable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DifferentialDiagnosisCommand.class);

    @CommandLine.Option(names = {"-m", "--maxoData"},
            required = true,
            description = "Path to MaXo data directory.")
    protected Path maxoDataPath;

    @CommandLine.Option(names = {"-p", "--phenopacket"},
//            required = true,
            arity = "1..*",
            description = "Path(s) to phenopacket JSON file(s).")
    protected Path phenopacketPath;

    @CommandLine.Option(names = {"-r", "--liricalResults"},
//            required = true,
            arity = "1..*",
            description = "Path(s) to LIRICAL output TSV file(s).")
    protected Path liricalResultsPath;

    @CommandLine.Option(names = {"-O", "--outputDirectory"},
//            required = true,
            description = "Where to write the results files.")
    protected Path outputDir;

    @CommandLine.Option(names = {"-w", "--weight"},
            split=",",
            arity = "1..*",
            description = "Comma-separated list of weight value to use in final score calculation (default: ${DEFAULT-VALUE}).")
    public List<Double> weightsArg;

    @CommandLine.Option(names = {"-l", "--diseaseList"},
//            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of diseases to include in differential diagnosis.")
    protected List<String> diseaseIdsArg;

    @CommandLine.Option(names = {"-t", "--threshold"},
//            required = true,
            split=",",
            arity = "1..*",
            description = "Comma-separated list of posttest probability thresholds for filtering diseases to include in differential diagnosis.")
    protected List<Double> thresholdsArg;


    public Integer call() throws Exception {

        Map<String, List<Object>> resultsMap = new HashMap<>();

        resultsMap.put("phenopacketName", new ArrayList<>());
        resultsMap.put("liricalOutputName", new ArrayList<>());
        resultsMap.put("diseaseId", new ArrayList<>());
        resultsMap.put("maxScoreMaxoTermId", new ArrayList<>());
        resultsMap.put("maxScoreTermLabel", new ArrayList<>());
        resultsMap.put("threshold", new ArrayList<>());
        resultsMap.put("topNDiseases", new ArrayList<>());
        resultsMap.put("diseaseIds", new ArrayList<>());
        resultsMap.put("weight", new ArrayList<>());
        resultsMap.put("maxScoreValue", new ArrayList<>());

        MaxoTermScoreMap maxoTermScoreMap = new MaxoTermScoreMap(maxoDataPath);

        List<Double> weights = new ArrayList<>();
        weightsArg.stream().forEach(w -> weights.add(w));
        List<Double> filterPosttestProbs = new ArrayList<>();
        thresholdsArg.stream().forEach(t -> filterPosttestProbs.add(t));

        try {
            // Load LIRICAL results file
            List<LiricalResultsFileRecord> liricalResultsFileRecords = LiricalResultsFileParser.read(liricalResultsPath);

            String phenopacketName = phenopacketPath.toFile().getName();
            String liricalOutputName = liricalResultsPath.toFile().getName();
            System.out.println(phenopacketName + ": " + liricalOutputName);

            //TODO? get list of diseases from LIRICAL results, and add diseases from CLI arg to total list for analysis

            for (double posttestFilter : filterPosttestProbs) {
                System.out.println("Min Posttest Probabiltiy Threshold = " + posttestFilter);
                // Make MaXo:HPO Term Map
                Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoTermMap = maxoTermScoreMap.makeMaxoToHpoTermMap(liricalResultsFileRecords,
                        phenopacketPath, posttestFilter);

                System.out.println(String.valueOf(maxoToHpoTermMap));

                for (double weight : weights) {
                    System.out.println("Weight = " + weight);
                    // Make map of MaXo scores
                    Map<SimpleTerm, Double> maxoScoreMap = maxoTermScoreMap.makeMaxoScoreMap(maxoToHpoTermMap, liricalResultsFileRecords, weight);
                    System.out.println(String.valueOf(maxoScoreMap));
                    // Take the MaXo term that has the highest score
                    Map.Entry<SimpleTerm, Double> maxScore = maxoScoreMap.entrySet().stream().max(Map.Entry.comparingByValue()).get();
                    TermId maxScoreMaxoTermId = maxScore.getKey().tid();
                    String maxScoreTermLabel = maxScore.getKey().label();
                    double maxScoreValue = maxScore.getValue();

                    System.out.println("Max Score: " + maxScoreMaxoTermId + " (" + maxScoreTermLabel + ")" + " = " + maxScoreValue);
//                    double finalScore = diffDiag.finalScore(results, diseaseIds, weight);
//                    LOGGER.info("Input Disease List Score: " + finalScore);
                    TermId diseaseId = maxoTermScoreMap.getDiseaseId();
                    Set<TermId> diseaseIds = new HashSet<>();
                    maxoTermScoreMap.getDiseases().forEach(disease -> diseaseIds.add(disease.id()));
                    int topNDiseases = diseaseIds.size();

                    List<Object> phenopacketNames = resultsMap.get("phenopacketName");
                    phenopacketNames.add(phenopacketName);
                    List<Object> liricalOutputNames = resultsMap.get("liricalOutputName");
                    liricalOutputNames.add(liricalOutputName);
                    List<Object> diseaseIdList = resultsMap.get("diseaseId");
                    diseaseIdList.add(diseaseId);
                    List<Object> maxScoreMaxoTermIds = resultsMap.get("maxScoreMaxoTermId");
                    maxScoreMaxoTermIds.add(maxScoreMaxoTermId);
                    List<Object> maxScoreTermLabels = resultsMap.get("maxScoreTermLabel");
                    maxScoreTermLabels.add(maxScoreTermLabel);
                    List<Object> posttestFilters = resultsMap.get("threshold");
                    posttestFilters.add(posttestFilter);
                    List<Object> topNDiseasesList = resultsMap.get("topNDiseases");
                    topNDiseasesList.add(topNDiseases);
                    List<Object> diseaseIdsList = resultsMap.get("diseaseIds");
                    diseaseIdsList.add(diseaseIds);
                    List<Object> weightList = resultsMap.get("weight");
                    weightList.add(weight);
                    List<Object> maxScoreValues = resultsMap.get("maxScoreValue");
                    maxScoreValues.add(maxScoreValue);
                    resultsMap.replace("phenopacketName", phenopacketNames);
                    resultsMap.replace("liricalOutputName", liricalOutputNames);
                    resultsMap.replace("diseaseId", diseaseIdList);
                    resultsMap.replace("maxScoreMaxoTermId", maxScoreMaxoTermIds);
                    resultsMap.replace("maxScoreTermLabel", maxScoreTermLabels);
                    resultsMap.replace("threshold", posttestFilters);
                    resultsMap.replace("topNDiseases", topNDiseasesList);
                    resultsMap.replace("diseaseIds", diseaseIdsList);
                    resultsMap.replace("weight", weightList);
                    resultsMap.replace("maxScoreValue", maxScoreValues);
                }
            }
            BatchDiagnosisCommand.setResultsMap(resultsMap);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            resultsMap = new HashMap<>();
            BatchDiagnosisCommand.setResultsMap(resultsMap);
        }

        return 0;
    }

}
