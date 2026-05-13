package org.p2gx.maxodiff.html.results.tleaf;

import org.p2gx.maxodiff.core.analysis.HTMLFrequencyMap;
import org.p2gx.maxodiff.core.analysis.MdMetadata;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;

import org.p2gx.maxodiff.html.results.maxoDisease.MdDiseaseHTML;
import org.p2gx.maxodiff.html.results.maxoHpo.MdHtmlResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.List;

public class TleafResults {
    private static final Logger LOGGER = LoggerFactory.getLogger(TleafResults.class);

    public static String writeHTMLResults(
            MdMetadata mdMetadata,
            List<RankedMaxoResult> resultList,
            HTMLFrequencyMap  htmlFrequencyMap)  {

        LOGGER.debug("Making Spring Template Engine.");
        SpringTemplateEngine templateEngine = templateEngine();

        Context context = new Context();
        System.out.println("OBSE" + mdMetadata.observedHpoTerms());
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
        LOGGER.debug("Get Top " + nDisplayed + " results.");
        List<RankedMaxoResult> results = resultList.subList(0, nDisplayed);

        // Disease : MAxO term result box
        LOGGER.debug("Make Disease:MAxO term result box.");
        MdDiseaseHTML mdDiseaseData = new MdDiseaseHTML(results);
        context.setVariable("maxoDiseaseData", mdDiseaseData);

        String mdDiseaseBox = templateEngine.process("mdDiseaseBox", context);
        context.setVariable("mdDiseaseBox", mdDiseaseBox);

        // Disease : MAxO HPO result box
        StringBuilder resultsString = new StringBuilder();
        for (RankedMaxoResult result : results) {
            int idx = resultList.indexOf(result) + 1;
            LOGGER.debug("Make Disease:MAxO term result box " + results.indexOf(result) + " of " + results.size());
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
        LOGGER.debug("Process template");
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
