package org.monarchinitiative.maxodiff.html.results;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HtmlResults {

    public static String writeHTMLResults(Sample sample, int nDiseases, int nRepetitions, List<MaxodiffResult> resultList,
                                           BiometadataService biometadataService, Map<TermId, List<HpoFrequency>> hpoTermCounts) throws Exception {

        File file = new File("maxodiff-html-results/src/main/resources/templates/maxodiffResults.html");
        String htmlTemplateFile = file.getAbsolutePath();
        Path htmlTemplatePath = Path.of(htmlTemplateFile);
        String htmlString = Files.readString(htmlTemplatePath);

        String sampleId = sample.id();
        String presentHpoIds = sample.presentHpoTermIds().toString();
        String excludedHpoIds = sample.excludedHpoTermIds().toString();
        htmlString = htmlString.replace("$sampleResultsTitle", "Maxodiff Analysis Results for " + sampleId);
        htmlString = htmlString.replace("$samplePresentHpoIds", presentHpoIds);
        htmlString = htmlString.replace("$sampleExcludedHpoIds", excludedHpoIds);
        htmlString = htmlString.replace("$nDiseases", String.valueOf(nDiseases));
        htmlString = htmlString.replace("$nRepetitions", String.valueOf(nRepetitions));

        String resultsString = getHTMLResults(resultList, biometadataService, nDiseases, nRepetitions, hpoTermCounts);

        htmlString = htmlString.replace("$results", resultsString);

        return htmlString;

    }

    protected static String getHTMLResults(List<MaxodiffResult> resultList, BiometadataService biometadataService,
                                           int nDiseases, int nRepetitions, Map<TermId, List<HpoFrequency>> hpoTermCounts) throws Exception {

        Map<TermId, String> hpoTermsMap = new HashMap<>();
        Map<TermId, String> omimTerms = new LinkedHashMap<>();
        StringBuilder resultsString = new StringBuilder();

        for (MaxodiffResult result : resultList.subList(0, 10)) {
            result.rankMaxoScore().discoverableHpoTermIds()
                    .forEach(id -> hpoTermsMap.put(id, biometadataService.hpoLabel(id).orElse("unknown")));
            result.rankMaxoScore().initialOmimTermIds()
                    .forEach(id -> omimTerms.put(id, biometadataService.diseaseLabel(id).orElse("unknown")));
            result.rankMaxoScore().maxoOmimTermIds()
                    .forEach(id -> omimTerms.put(id, biometadataService.diseaseLabel(id).orElse("unknown")));

            int idx = resultList.indexOf(result) + 1;
            String maxoId = result.maxoTermScore().maxoId();
            String maxoLabel = biometadataService.maxoLabel(maxoId).orElse("unknown");
            String maxoTermHeader = idx + ") " + maxoId + ": " + maxoLabel;

            resultsString.append("<h2>").append(maxoTermHeader).append("</h2>\n\n");

            resultsString.append("<table style='border: 1px solid black;\n" +
                            "    background-color: peachpuff;'>\n  <tbody>\n    <tr>\n      " +
                            "<td style='font-weight:bold;'>&Delta;Score:</td><td>")
                    .append(String.format("%.2f", result.rankMaxoScore().maxoScore()))
                    .append("</td>\n    </tr>\n    <tr><td style='font-weight:bold;'>N Diseases:</td><td>")
                    .append(result.rankMaxoScore().maxoOmimTermIds().size())
                    .append("</td>\n    </tr>\n    <tr><td style='font-weight:bold;'>N HPO Terms:</td><td>")
                    .append(result.rankMaxoScore().discoverableHpoTermIds().size())
                    .append("</td>\n    </tr>\n  </tbody>\n</table>\n<p></p>\n\n");

            resultsString.append("<table>\n  <tbody>\n    <tr>\n      <td><div id=nRepHeatmapChartContainer_").append(idx)
                    .append(" style=\"height:600px; width:1000px; border: 1px solid black\"></div>" +
                            "</td>\n    </tr>\n  </tbody>\n</table>\n\n");

            resultsString.append("<script src=\"https://cdn.jsdelivr.net/npm/apexcharts\"></script>\n");
            String hpoTermIdRepCtsMap = convertToJson(result.rankMaxoScore().hpoTermIdRepCtsMap());
            String maxoDiseaseAvgRankChangeMap = convertToJson(result.rankMaxoScore().maxoDiseaseAvgRankChangeMap());
            String allHpoTermsMap = convertToJson(hpoTermsMap);
            String omimTermsMap = convertToJson(omimTerms);
            String hpoTermCountsMap = convertToJson(hpoTermCounts);
            resultsString.append("<script inline=\"javascript\">\n" +
                    "    var chartIdx = ").append(idx).append(";\n" +
                    "    var nDiseases = ").append(nDiseases).append(";\n" +
                    "    var nRepetitions = ").append(nRepetitions).append(";\n" +
                    "    var hpoTermIdRepCtsMap = ").append(hpoTermIdRepCtsMap).append(";\n" +
                    "    var maxoDiseaseAvgRankChangeMap = ").append(maxoDiseaseAvgRankChangeMap).append(";\n" +
                    "    var allHpoTermsMap = ").append(allHpoTermsMap).append(";\n" +
                    "    var omimTerms = ").append(omimTermsMap).append(";\n" +
                    "    var hpoTermCounts = ").append(hpoTermCountsMap).append(";\n" +
                    "</script>\n");

            String jsChartName = "nRepetitionsHeatmapChart.js";
            File chartFile = new File(jsChartName);
            File chartPath = new File(chartFile.getAbsolutePath());
            String parentPath = chartPath.getParent();
            String path = String.join(File.separator, parentPath,
                    "maxodiff-html-results", "src", "main", "resources", "static", "js", jsChartName);
            resultsString.append("<script type=\"text/javascript\" src=\"").append(path).append("\"></script>\n\n");

        }

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
