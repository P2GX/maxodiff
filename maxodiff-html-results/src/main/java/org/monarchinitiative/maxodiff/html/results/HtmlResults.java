package org.monarchinitiative.maxodiff.html.results;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.maxodiff.html.results.maxoDisease.MaxoDiseaseHTML;
import org.monarchinitiative.maxodiff.html.results.maxoHpo.MaxoHtmlResult;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class HtmlResults {

    public static String writeHTMLResults(Sample sample, int nDiseases, int nRepetitions, List<MaxodiffResult> resultList,
                                           BiometadataService biometadataService, Map<TermId, List<HpoFrequency>> hpoTermCounts) throws Exception {

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        String sampleId = sample.id();
        List<String> observedHpoLinks = sample.presentHpoTermIds().stream().map(tid -> hpoLink(tid,biometadataService)).toList();
        String samplePresentTermsString = String.join(" ", observedHpoLinks);
        List<String> excludedHpoLinks = sample.excludedHpoTermIds().stream().map(tid -> hpoLink(tid,biometadataService)).toList();
        String sampleExcludedTermsString = String.join(" ", excludedHpoLinks);

        String resultsString = getHTMLResults(resultList, biometadataService, nDiseases, nRepetitions, hpoTermCounts);

        MaxodiffHtml maxodiffHtml = new MaxodiffHtml(sampleId, samplePresentTermsString, sampleExcludedTermsString,
                nDiseases, nRepetitions, resultsString);

        Context context = new Context();
        context.setVariable("maxodiff", maxodiffHtml);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        return templateEngine.process("maxodiffResults", context);

    }

    public static String writeHTMLMaxoDiseaseResults(Sample sample, int nDiseases, int nRepetitions, List<MaxodiffResult> resultList,
                                          BiometadataService biometadataService) throws Exception {

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        String sampleId = sample.id();
        StringBuilder sampleObservedTermsStringBuilder = new StringBuilder();
        sample.presentHpoTermIds().forEach(tid -> sampleObservedTermsStringBuilder
                .append(hpoLink(tid,biometadataService)).append(" "));
        String samplePresentTermsString = sample.presentHpoTermIds().isEmpty() ? "" :
                sampleObservedTermsStringBuilder.substring(0, sampleObservedTermsStringBuilder.length() - 2);
        StringBuilder sampleExcludedTermsStringBuilder = new StringBuilder();
        sample.excludedHpoTermIds().forEach(tid -> sampleExcludedTermsStringBuilder
                .append(hpoLink(tid,biometadataService)).append(" "));
        String sampleExcludedTermsString = sample.excludedHpoTermIds().isEmpty() ? "" :
                sampleExcludedTermsStringBuilder.substring(0, sampleExcludedTermsStringBuilder.length() - 2);

        String resultsString = getHTMLMaxoDiseaseResults(resultList, biometadataService);

        MaxodiffHtml maxodiffHtml = new MaxodiffHtml(sampleId, samplePresentTermsString, sampleExcludedTermsString,
                                                nDiseases, nRepetitions, resultsString);


        Context context = new Context();
        context.setVariable("maxodiff", maxodiffHtml);
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        return templateEngine.process("maxodiffResults", context);
    }

    private static String hpoLink(TermId tid, BiometadataService biometadataService) {
        String label = biometadataService.hpoLabel(tid).orElse("n/a");
        return String.format("<a href=\"https://hpo.jax.org/browse/term/%s\" target=\"_blank\">%s</a>", tid.getValue(), label);
    }

    static String getHTMLboxFromTemplate(MaxodiffResult result,
                                         BiometadataService biometadataService,
                                         int nDiseases,
                                         int nRepetitions,
                                         Map<TermId, List<HpoFrequency>> hpoTermCountMap,
                                         int idx, SpringTemplateEngine templateEngine) {

        MaxoHtmlResult maxoData = new MaxoHtmlResult(
                result,
                hpoTermCountMap,
                idx,
                nDiseases,
                nRepetitions,
                biometadataService
        );
        Context context = new Context();
        context.setVariable("maxoData", maxoData);
        return templateEngine.process("maxoResultBox", context);
    }

    protected static String getHTMLResults(List<MaxodiffResult> resultList, BiometadataService biometadataService,
                                           int nDiseases, int nRepetitions, Map<TermId, List<HpoFrequency>> hpoTermCounts) throws Exception {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        StringBuilder resultsString = new StringBuilder();

        int zeroIdx = resultList.stream()
                .filter(result -> result.rankMaxoScore().maxoScore().equals(0.))
                .findFirst().map(resultList::indexOf).orElse(0);
        int nDisplayed = Math.min(resultList.size(), zeroIdx);
        for (MaxodiffResult result : resultList.subList(0, nDisplayed)) {
            int idx = resultList.indexOf(result) + 1;
           // String html = getHTMLbox(result, biometadataService, nDiseases, nRepetitions, hpoTermCounts, idx);
            String templateHtml = getHTMLboxFromTemplate(result,
                    biometadataService,
                    nDiseases,
                    nRepetitions,
                    hpoTermCounts,
                    idx,
                    templateEngine);
            resultsString.append(templateHtml);
        }

        return resultsString.toString();
    }

    static String getHTMLMaxoDiseaseBoxFromTemplate(List<MaxodiffResult> results,
                                                    BiometadataService biometadataService,
                                                    SpringTemplateEngine templateEngine) {

        MaxoDiseaseHTML maxoDiseaseData = new MaxoDiseaseHTML(
                results,
                biometadataService
        );
        Context context = new Context();
        context.setVariable("maxoDiseaseData", maxoDiseaseData);
        return templateEngine.process("maxoDiseaseResultBox", context);
    }

    protected static String getHTMLMaxoDiseaseResults(List<MaxodiffResult> resultList, BiometadataService biometadataService) throws Exception {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);
        StringBuilder resultsString = new StringBuilder();

        int zeroIdx = resultList.stream()
                .filter(result -> result.rankMaxoScore().maxoScore().equals(0.))
                .findFirst().map(resultList::indexOf).orElse(0);
        int nDisplayed = Math.min(resultList.size(), zeroIdx);
        List<MaxodiffResult> results = resultList.subList(0, nDisplayed);
        String templateHtml = getHTMLMaxoDiseaseBoxFromTemplate(results,
                biometadataService,
                templateEngine);
        resultsString.append(templateHtml);

        return resultsString.toString();
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
