package org.monarchinitiative.maxodiff.html.results.tleaf;

import org.monarchinitiative.maxodiff.core.analysis.HTMLFrequencyMap;
import org.monarchinitiative.maxodiff.core.analysis.MdMetadata;
import org.monarchinitiative.maxodiff.core.analysis.RankedMaxoResult;

import org.monarchinitiative.maxodiff.html.results.maxoDisease.MdDiseaseHTML;
import org.monarchinitiative.maxodiff.html.results.maxoHpo.MdHtmlResult;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.List;

public class TleafResults {

    public static String writeHTMLResults(
            MdMetadata mdMetadata,
            List<RankedMaxoResult> resultList,
            HTMLFrequencyMap  htmlFrequencyMap)  {

        SpringTemplateEngine templateEngine = templateEngine();

        Context context = new Context();

        // Header
        context.setVariable("ppktId", mdMetadata.ppktId());
        context.setVariable("observedHpoTerms", mdMetadata.observedHpoTerms());
        context.setVariable("excludedHpoTerms", mdMetadata.excludedHpoTerms());
        context.setVariable("nDiseases", mdMetadata.nDiseases());
        context.setVariable("nRepetitions", mdMetadata.nRepetitions());

        String mdHeader = templateEngine.process("mdHeader", context);
        context.setVariable("mdHeader", mdHeader);

        int zeroIdx = resultList.stream()
                .filter(result -> result.maxoScore() == 0.)
                .findFirst().map(resultList::indexOf).orElse(resultList.size());
        int nDisplayed = Math.min(resultList.size(), zeroIdx);
        List<RankedMaxoResult> results = resultList.subList(0, nDisplayed);

        // Disease : MAxO term result box
        MdDiseaseHTML mdDiseaseData = new MdDiseaseHTML(results);
        context.setVariable("maxoDiseaseData", mdDiseaseData);

        String mdDiseaseBox = templateEngine.process("mdDiseaseBox", context);
        context.setVariable("mdDiseaseBox", mdDiseaseBox);

        // Disease : MAxO HPO result box
        StringBuilder resultsString = new StringBuilder();
        for (RankedMaxoResult result : results) {
            int idx = resultList.indexOf(result) + 1;
            MdHtmlResult maxoData = new MdHtmlResult(
                    result,
                    idx,
                    mdMetadata.nDiseases(),
                    mdMetadata.nRepetitions(),
                    htmlFrequencyMap
            );
            context.setVariable("maxoData", maxoData);
            String mdHeatmap = templateEngine.process("mdHeatmap", context);
            resultsString.append(mdHeatmap);
        }

        context.setVariable("maxoResults", resultsString);
        return templateEngine.process("mdResults", context);

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
}
