package org.p2gx.maxodiff.html.results.tleaf;

import org.p2gx.maxodiff.core.analysis.HTMLFrequencyMap;
import org.p2gx.maxodiff.core.analysis.MdMetadata;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;

import org.p2gx.maxodiff.html.results.maxoDisease.MdDiseaseHTML;
import org.p2gx.maxodiff.html.results.maxoHpo.MdHtmlResult;
import org.p2gx.maxodiff.html.results.summary.MdSummaryRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TleafResults {
    private static final Logger LOGGER = LoggerFactory.getLogger(TleafResults.class);

    /** Number of MAxO terms that are shown in full detail; the others are listed in a summary table. */
    public static final int DEFAULT_N_DETAILED = 10;

    public static String writeHTMLResults(MdMetadata mdMetadata,
                                          List<RankedMaxoResult> resultList,
                                          HTMLFrequencyMap htmlFrequencyMap) {
        return writeHTMLResults(mdMetadata, resultList, htmlFrequencyMap, DEFAULT_N_DETAILED);
    }

    public static String writeHTMLResults(
            MdMetadata mdMetadata,
            List<RankedMaxoResult> resultList,
            HTMLFrequencyMap  htmlFrequencyMap,
            int nDetailed)  {
        if (nDetailed < 0) {
            throw new IllegalArgumentException("nDetailed must not be negative but was " + nDetailed);
        }
        SpringTemplateEngine templateEngine = templateEngine();
        Context context = new Context(Locale.ENGLISH);

        // Header
        context.setVariable("ppktId", mdMetadata.ppktId());
        context.setVariable("observedHpoTerms", mdMetadata.observedHpoTerms());
        context.setVariable("excludedHpoTerms", mdMetadata.excludedHpoTerms());
        context.setVariable("nDiseases", mdMetadata.nDiseases());
        context.setVariable("nRepetitions", mdMetadata.nRepetitions());

        String mdHeader = templateEngine.process("mdHeader", context);
        context.setVariable("mdHeader", mdHeader);
        // The results are sorted by score; the list ends before the first test with a score of 0
        int zeroIdx = resultList.stream()
                .filter(result -> result.maxoScore() == 0.)
                .findFirst().map(resultList::indexOf).orElse(resultList.size());
        int nDisplayed = Math.min(resultList.size(), zeroIdx);
        List<RankedMaxoResult> results = resultList.subList(0, Math.min(resultList.size(), zeroIdx));
        List<RankedMaxoResult> detailed = results.subList(0, Math.min(nDetailed, results.size()));
        List<RankedMaxoResult> others = results.subList(detailed.size(), results.size());
        LOGGER.debug("{} results, {} in detail, {} summarised.", results.size(), detailed.size(), others.size());
        context.setVariable("nTotal", results.size());
        context.setVariable("nDetailed", detailed.size());
        // Disease : MAxO term result box
        LOGGER.debug("Make Disease:MAxO term result box.");
        String mdDiseaseBox = "";
        if (!detailed.isEmpty()) {
            context.setVariable("maxoDiseaseData", new MdDiseaseHTML(detailed));
            mdDiseaseBox = templateEngine.process("mdDiseaseBox", context);
        }
        context.setVariable("mdDiseaseBox", mdDiseaseBox);

        // One box with all details for each of the best terms
        StringBuilder resultsString = new StringBuilder();
        double maxMica = htmlFrequencyMap.maxMica();
        for (int i = 0; i < detailed.size(); i++) {
            MdHtmlResult maxoData = new MdHtmlResult(detailed.get(i), i + 1, mdMetadata.nDiseases(),
                    mdMetadata.nRepetitions(), htmlFrequencyMap, maxMica);
            context.setVariable("maxoData", maxoData);
            resultsString.append(templateEngine.process("mdHeatmap", context));
        }
        context.setVariable("maxoResults", resultsString);

        // A short table for the other terms
        List<MdSummaryRow> summaryRows = new ArrayList<>();
        for (int i = 0; i < others.size(); i++) {
            summaryRows.add(MdSummaryRow.of(detailed.size() + i + 1, others.get(i)));
        }
        context.setVariable("otherTerms", summaryRows);
        context.setVariable("mdOtherTerms", summaryRows.isEmpty() ? "" : templateEngine.process("mdOtherTerms", context));

        return templateEngine.process("mdResults", context);
    }
    /**
     * @return Spring thymeleaf template engine
     */
    private static SpringTemplateEngine templateEngine() {
        return TEMPLATE_ENGINE;
    }

    private static final SpringTemplateEngine TEMPLATE_ENGINE = createTemplateEngine();

    private static SpringTemplateEngine createTemplateEngine() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");
        templateResolver.setCharacterEncoding("UTF-8");
        templateResolver.setCacheable(true);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(templateResolver);
        return engine;
    }
}
