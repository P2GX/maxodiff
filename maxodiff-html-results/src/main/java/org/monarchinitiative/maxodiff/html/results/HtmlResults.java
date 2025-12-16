package org.monarchinitiative.maxodiff.html.results;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.monarchinitiative.maxodiff.core.analysis.HTMLFrequencyMap;
import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.io.JsonFileWriter;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.html.results.maxoDisease.MaxoDiseaseHTML;
import org.monarchinitiative.maxodiff.html.results.maxoHpo.MaxoHtmlResult;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.*;

public class HtmlResults {

    public static String writeHTMLResults(
            Sample sample,
            int nDiseases,
            HpoDiseases diseases,
            int nRepetitions,
            List<MaxodiffResult> resultList,
            BiometadataService biometadataService,
            Map<TermId, List<HpoFrequency>> hpoTermCounts,
            Map<TermPair, Double> icMicaData,
            Path outputDir,
            boolean writeJson) throws Exception {

        SpringTemplateEngine templateEngine = templateEngine();

        String sampleId = sample.id();
        List<String> observedHpoLinks = sample.observedHpoTermIds().stream().map(tid -> hpoLink(tid,biometadataService)).toList();
        String samplePresentTermsString = String.join(" ", observedHpoLinks);
        List<String> excludedHpoLinks = sample.excludedHpoTermIds().stream().map(tid -> hpoLink(tid,biometadataService)).toList();
        String sampleExcludedTermsString = String.join(" ", excludedHpoLinks);

        HTMLFrequencyMap htmlFrequencyMap = new HTMLFrequencyMap(diseases, icMicaData);

        String resultsString = getHTMLResults(
                resultList,
                biometadataService,
                sample,
                nDiseases,
                nRepetitions,
                hpoTermCounts,
                htmlFrequencyMap,
                outputDir,
                writeJson);

        MaxodiffHtml maxodiffHtml = new MaxodiffHtml(
                sampleId,
                samplePresentTermsString,
                sampleExcludedTermsString,
                nDiseases,
                nRepetitions,
                resultsString);

        if (writeJson) {
            String htmlJsonString = HtmlToJson.convertHtmlToJson(maxodiffHtml);

            String nDiseasesAbbr = String.join("", "n", String.valueOf(nDiseases));
            String nRepsAbbr = String.join("", "nr", String.valueOf(nRepetitions));
            String outputFilename = String.join("_", sample.id(),
                    nDiseasesAbbr, nRepsAbbr, "maxodiff", "html.json");
            Path maxodiffHtmlJsonPath = Path.of(String.join(File.separator, outputDir.toString(), outputFilename));

            JsonFileWriter.writeToJsonFile(maxodiffHtmlJsonPath, htmlJsonString);
        }

        Context context = new Context();
        context.setVariable("maxodiff", maxodiffHtml);
        return templateEngine.process("maxodiffResults", context);

    }

    private static String hpoLink(TermId tid, BiometadataService biometadataService) {
        String label = biometadataService.hpoLabel(tid).orElse("n/a");
        return String.format("<a href=\"https://hpo.jax.org/browse/term/%s\" target=\"_blank\">%s</a>", tid.getValue(), label);
    }

    static String getHTMLboxFromTemplate(MaxodiffResult result,
                                         BiometadataService biometadataService,
                                         Sample sample,
                                         int nDiseases,
                                         int nRepetitions,
                                         Map<TermId, List<HpoFrequency>> hpoTermCountMap,
                                         int idx,
                                         HTMLFrequencyMap htmlFrequencyMap,
                                         SpringTemplateEngine templateEngine,
                                         Path outputDir,
                                         boolean writeJson) throws IOException {

        MaxoHtmlResult maxoData = new MaxoHtmlResult(
                result,
                hpoTermCountMap,
                idx,
                nDiseases,
                nRepetitions,
                biometadataService,
                htmlFrequencyMap
        );

        if (writeJson) {
            String htmlResultsJsonString = HtmlToJson.convertHtmlResultsToJson(maxoData);

            String nDiseasesAbbr = String.join("", "n", String.valueOf(nDiseases));
            String nRepsAbbr = String.join("", "nr", String.valueOf(nRepetitions));

            String outputFilename = String.join("_", sample.id(),
                    nDiseasesAbbr, nRepsAbbr, "maxodiff", "html", "researcher", "results.json");
            Path maxodiffHtmlJsonPath = Path.of(String.join(File.separator, outputDir.toString(), outputFilename));

            JsonFileWriter.writeToJsonFile(maxodiffHtmlJsonPath, htmlResultsJsonString);
        }

        Context context = new Context();
        context.setVariable("maxoData", maxoData);
        return templateEngine.process("maxoResultBox", context);
    }

    /**
     * @return Spring thymeleaf template engine
     */
    static private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine;
    }


    protected static String getHTMLResults(
            List<MaxodiffResult> resultList,
            BiometadataService biometadataService,
            Sample sample,
            int nDiseases,
            int nRepetitions,
            Map<TermId, List<HpoFrequency>> hpoTermCounts,
            HTMLFrequencyMap htmlFrequencyMap,
            Path outputDir,
            boolean writeJson) throws IOException {
        SpringTemplateEngine templateEngine = templateEngine();
        StringBuilder resultsString = new StringBuilder();

        int zeroIdx = resultList.stream()
                .filter(result -> result.rankMaxoScore().maxoScore().equals(0.))
                .findFirst().map(resultList::indexOf).orElse(resultList.size());
        int nDisplayed = Math.min(resultList.size(), zeroIdx);

        // Clinician view results: MAxO terms vs. Diseases
        List<MaxodiffResult> results = resultList.subList(0, nDisplayed);
        String templateHtml0 = getHTMLMaxoDiseaseBoxFromTemplate(results,
                biometadataService,
                templateEngine,
                sample,
                nDiseases,
                nRepetitions,
                outputDir,
                writeJson);
        resultsString.append(templateHtml0);

        // Researcher view results: HPO terms vs. Diseases
        for (MaxodiffResult result : resultList.subList(0, nDisplayed)) {
            int idx = resultList.indexOf(result) + 1;
            String templateHtml = getHTMLboxFromTemplate(result,
                    biometadataService,
                    sample,
                    nDiseases,
                    nRepetitions,
                    hpoTermCounts,
                    idx,
                    htmlFrequencyMap,
                    templateEngine,
                    outputDir,
                    writeJson);
            resultsString.append(templateHtml);
        }

        return resultsString.toString();
    }

    static String getHTMLMaxoDiseaseBoxFromTemplate(List<MaxodiffResult> results,
                                                    BiometadataService biometadataService,
                                                    SpringTemplateEngine templateEngine,
                                                    Sample sample,
                                                    int nDiseases,
                                                    int nRepetitions,
                                                    Path outputDir,
                                                    boolean writeJson) throws IOException {

        MaxoDiseaseHTML maxoDiseaseData = new MaxoDiseaseHTML(
                results,
                biometadataService
        );

        if (writeJson) {
            String htmlDiseaseJsonString = HtmlToJson.convertDiseaseHtmlToJson(maxoDiseaseData);

            String nDiseasesAbbr = String.join("", "n", String.valueOf(nDiseases));
            String nRepsAbbr = String.join("", "nr", String.valueOf(nRepetitions));

            String outputFilename = String.join("_", sample.id(),
                    nDiseasesAbbr, nRepsAbbr, "maxodiff", "html", "clinician", "results.json");
            Path maxodiffHtmlJsonPath = Path.of(String.join(File.separator, outputDir.toString(), outputFilename));

            JsonFileWriter.writeToJsonFile(maxodiffHtmlJsonPath, htmlDiseaseJsonString);
        }


        Context context = new Context();
        context.setVariable("maxoDiseaseData", maxoDiseaseData);
        return templateEngine.process("maxoDiseaseResultBox", context);
    }

    protected static String convertToJson(Object object) throws Exception {
        ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        OBJECT_MAPPER.registerModule(new Jdk8Module());

        StringWriter writer = new StringWriter();
        JsonGenerator generator = OBJECT_MAPPER.createGenerator(writer);

        generator.writeObject(object);

        return writer.toString().replaceAll("\r", "");
    }
}
