package org.monarchinitiative.maxodiff.html.results.tleaf;

import org.monarchinitiative.maxodiff.core.analysis.MdMetadata;
import org.monarchinitiative.maxodiff.core.analysis.RankedMaxoResult;
import org.monarchinitiative.maxodiff.core.analysis.SimpleTerm;

import org.monarchinitiative.maxodiff.html.results.maxoDisease.MaxoDiseaseHTML;
import org.monarchinitiative.maxodiff.html.results.maxoDisease.MdDiseaseHTML;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.List;

public class TleafHeader {

    public static String writeHTMLResults(
            MdMetadata mdMetadata,
            List<RankedMaxoResult> resultList) throws Exception {

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

        // Disease : MAxO result box
        MdDiseaseHTML mdDiseaseData = new MdDiseaseHTML(resultList);
        context.setVariable("maxoDiseaseData", mdDiseaseData);

        String mdDiseaseBox = templateEngine.process("mdDiseaseBox", context);
        context.setVariable("mdDiseaseBox", mdDiseaseBox);

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
