package org.monarchinitiative.maxodiff.html.results.tleaf;

import org.monarchinitiative.maxodiff.core.analysis.MdMetadata;
import org.monarchinitiative.maxodiff.core.analysis.SimpleTerm;

import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.List;

public class TleafHeader {

    public static String writeHTMLResults(
            MdMetadata mdMetadata) throws Exception {

        SpringTemplateEngine templateEngine = templateEngine();

        String sampleId = mdMetadata.ppktId();
        List<SimpleTerm> observedHpoLinks = mdMetadata.observedHpoTerms();
        List<SimpleTerm> excludedHpoLinks = mdMetadata.excludedHpoTerms();

        Context context = new Context();
        context.setVariable("ppktId", sampleId);
        context.setVariable("observedHpoIds", observedHpoLinks);
        context.setVariable("excludedHpoIds", excludedHpoLinks);
        context.setVariable("nDiseases", mdMetadata.nDiseases());
        context.setVariable("nRepetitions", mdMetadata.nRepetitions());
        return templateEngine.process("mdHeader", context);

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
