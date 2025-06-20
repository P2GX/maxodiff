package org.monarchinitiative.maxodiff.html.results;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.monarchinitiative.maxodiff.core.analysis.HTMLFrequencyMap;
import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.io.File;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class HtmlResults {

    public static String writeHTMLResults(Sample sample, int nDiseases, int nRepetitions, List<MaxodiffResult> resultList,
                                           BiometadataService biometadataService, Map<TermId, List<HpoFrequency>> hpoTermCounts) throws Exception {

        File file = new File("maxodiff-html-results/src/main/resources/templates/maxodiffResults.html");
        String htmlTemplateFile = file.getAbsolutePath();
        Path htmlTemplatePath = Path.of(htmlTemplateFile);
        String htmlString = Files.readString(htmlTemplatePath);

        String sampleId = sample.id();
        StringBuilder samplePresentTermsStringBuilder = new StringBuilder();
        sample.presentHpoTermIds().forEach(tid -> samplePresentTermsStringBuilder
                .append(biometadataService.hpoLabel(tid).orElse("unknown")).append(" (")
                .append(tid).append("), "));
        String samplePresentTermsString = samplePresentTermsStringBuilder.substring(0, samplePresentTermsStringBuilder.length() - 2);
        StringBuilder sampleExcludedTermsStringBuilder = new StringBuilder();
        sample.excludedHpoTermIds().forEach(tid -> sampleExcludedTermsStringBuilder
                .append(biometadataService.hpoLabel(tid).orElse("unknown")).append(" (")
                .append(tid).append("), "));
        String sampleExcludedTermsString = sampleExcludedTermsStringBuilder.substring(0, sampleExcludedTermsStringBuilder.length() - 2);

        htmlString = htmlString.replace("$sampleResultsTitle", "Maxodiff Analysis Results for " + sampleId);
        htmlString = htmlString.replace("$samplePresentHpoIds", samplePresentTermsString);
        htmlString = htmlString.replace("$sampleExcludedHpoIds", sampleExcludedTermsString);
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

        List<HpoFrequency> hpoFrequencies = HTMLFrequencyMap.getHpoFrequencies(hpoTermCounts);
        Map<String, Map<Float, List<String>>> frequencyMap = new HashMap<>();

        Map<TermId, Integer> nRepetitionsMap = new HashMap<>();

        for (MaxodiffResult result : resultList.subList(0, 10)) {
            result.rankMaxoScore().discoverableObservedHpoTermIds()
                    .forEach(id -> hpoTermsMap.put(id, biometadataService.hpoLabel(id).orElse("unknown")));
            result.rankMaxoScore().initialOmimTermIds()
                    .forEach(id -> omimTerms.put(id, biometadataService.diseaseLabel(id).orElse("unknown")));
            result.rankMaxoScore().maxoOmimTermIds()
                    .forEach(id -> omimTerms.put(id, biometadataService.diseaseLabel(id).orElse("unknown")));
            var hpoTermIdRepCtsMap = result.rankMaxoScore().hpoTermIdRepCtsMap();
            for (Map.Entry<TermId, Map<TermId, Integer>> diseaseHpoRepCtEntry : hpoTermIdRepCtsMap.entrySet()) {
                Map<TermId, Integer> hpoRetCtMap = diseaseHpoRepCtEntry.getValue();
                for (Map.Entry<TermId, Integer> hpoRepCtMapEntry : hpoRetCtMap.entrySet()) {
                    TermId hpoId = hpoRepCtMapEntry.getKey();
                    Integer repCt = hpoRepCtMapEntry.getValue();
                    if (repCt != null && !nRepetitionsMap.containsKey(hpoId)) {
                        nRepetitionsMap.put(hpoId, repCt);
                        break;
                    }
                }
            }

            Map<String, Map<Float, List<String>>> resultFrequencyMap = HTMLFrequencyMap.makeFrequencyDiseaseMap(hpoTermsMap, omimTerms, hpoTermIdRepCtsMap, hpoFrequencies);
            frequencyMap.putAll(resultFrequencyMap);

            int idx = resultList.indexOf(result) + 1;
            String maxoId = result.maxoTermScore().maxoId();
            String maxoLabel = biometadataService.maxoLabel(maxoId).orElse("unknown");
            String maxoTermHeader = idx + ") " + maxoId + ": " + maxoLabel;

            resultsString.append("<div class=\"section-box\">");
            resultsString.append("<h2>").append(maxoTermHeader).append("</h2>\n\n");

            resultsString.append("<table class=\"scoreTable\">\n  <tbody>\n    <tr>\n      " +
                            "<td>&Delta;Score:</td><td>")
                    .append(String.format("%.2f", result.rankMaxoScore().maxoScore()))
                    .append("</td>\n    </tr>\n    <tr><td>N Diseases:</td><td>")
                    .append(result.rankMaxoScore().maxoOmimTermIds().size())
                    .append("</td>\n    </tr>\n    <tr><td>N Observed HPO Terms:</td><td>")
                    .append(result.rankMaxoScore().discoverableObservedHpoTermIds().size())
                    .append("</td>\n    </tr>\n  </tbody>\n</table>\n<p></p>\n\n");


            String thStyleString = "text-align: center; vertical-align: bottom; horizontal-align: right;" +
                    " transform-origin: bottom left; transform: rotate(315deg); padding: 0; margin: 0;" +
                    " height: 200px; white-space: nowrap; max-width: 50px; overflow: visible; text-overflow: ellipsis;";
            String tdStyleString = "padding: 10px 10px 10px 10px; height: 30px; width: 10px;";
            String tdStyleString1 = "font-weight: bold; white-space: nowrap; max-width: 400px; overflow: hidden; text-overflow: ellipsis;";

            resultsString.append("<table class='countsTable'>\n" +
                    "        <thead>\n" +
                    "            <th></th>\n" +
                    "            <th><span style=\"text-decoration: overline;\">\n" +
                    "                <span style=\"font-weight: bold; color: blue; font-size: 1.0em;\">&#916;&thinsp;R</span>\n" +
                    "            </span></th>\n" +
                    "            <th></th>\n");

            for (TermId hpoId : result.rankMaxoScore().discoverableObservedHpoTermIds()) {
                String hpoLabelString = hpoTermsMap.get(hpoId);
                String hpoLabel = hpoLabelString.length() > 30 ? hpoLabelString.substring(0,30) + "..." : hpoLabelString;
                resultsString.append("                <th onclick=\"window.open('https://hpo.jax.org/browse/term/" + hpoId + "')\"\n" +
                        "                    style=\"color: blue; " + thStyleString + "\"><div>" + hpoLabel + "</div></th>\n");
            }

            resultsString.append("        </thead>\n" +
                    "\n");

            resultsString.append("        <tbody>\n" +
                    "        <tr>\n" +
                    "            <td style=\"" + tdStyleString + tdStyleString1 + "\">N Repetitions</td>\n" +
                    "            <td></td>\n" +
                    "            <td></td>\n");

            for (TermId hpoId : result.rankMaxoScore().discoverableObservedHpoTermIds()) {
                Integer ct = nRepetitionsMap.get(hpoId);
                String ctString = (ct == null) ? "" : ct.toString();
                double opacity = (ct == null) ? 0 : (ct*1.0)/nRepetitions;
                String styleString = "background: rgba(255, 215, 0, " + opacity + ")";
                String hpoLabel = hpoTermsMap.get(hpoId);
                var freqHTML = "";
                for (Map.Entry<String, Map<Float, List<String>>> freqDiseaseMapEntry : frequencyMap.entrySet()) {
                    String hpoLabelKey = freqDiseaseMapEntry.getKey();
                    Map<Float, List<String>> freqMapValue = freqDiseaseMapEntry.getValue();
                    if (Objects.equals(hpoLabelKey, hpoLabel)) {
                        for (Map.Entry<Float, List<String>> freqDiseaseMapValue : freqMapValue.entrySet()) {
                            Float frequency = freqDiseaseMapValue.getKey();
                            List<String> omimLabels = freqDiseaseMapValue.getValue();
                            String omimLabelString = String.join("; ", omimLabels);
                            freqHTML += "<div><b>Frequency of " + "<span style=\"color: red\">" + hpoLabel + "</span>" +
                                    " in " + "<span style=\"color: blue\">" + omimLabelString + "</span>" +
                                    "</b>: " + frequency + "</div>" +
                                    "<div><p></p></div>";
                        }
                    }
                }
                String toolTipString = "<div style=\"background-color: lightgray; color: red\"><b>HPO Term</b>: " + hpoLabel + "</div>" +
                        "<div><p></p></div>" + freqHTML +
                        "<div style=\"background-color: gold\"><b>Repetition Count</b>: " + ct + " of " + nRepetitions + "</div>";

                resultsString.append("                    <td class=\"parentCell\" style=\"" + styleString + "\">" + ctString +
                        "<span class=\"tooltip\">" + toolTipString + "</span></td>\n");
            }

            resultsString.append("        </tr>\n");

            String divStyleString = "width: 80%; height: 80%; margin: auto;";
            for (TermId omimId : result.rankMaxoScore().maxoDiseaseAvgRankChangeMap().keySet()) {
                resultsString.append("            <tr>\n" +
                        "                <td style=\"" + tdStyleString + tdStyleString1 + "\">" + omimTerms.get(omimId) + "</td>\n");

                var rankChange = result.rankMaxoScore().maxoDiseaseAvgRankChangeMap().get(omimId);
                double opacity = (rankChange != null) ? (rankChange*1.0)/nDiseases : 0;
                String styleString = (rankChange < 0) ? "background: rgba(0, 128, 0, " + (-1.0*opacity) + ")" :
                        "background: rgba(255, 0, 0, " + opacity + ")";
                String tooltipString = (rankChange < 0) ? "<div style=\"background-color: lightgray; color: blue\">" +
                        "<b>Disease Term</b>: " + omimTerms.get(omimId) + "</div>" +
                        "<div><p></p></div>" +
                        "<div><b>Average Rank Improvement</b>: " + -rankChange + "</div>" :
                        "<div style=\"background-color: lightgray; color: blue\">" +
                                "<b>Disease Term</b>: " + omimTerms.get(omimId) + "</div>" +
                                "<div><p></p></div>" +
                                "<div><b>Average Rank Decline</b>: " + rankChange + "</div>";

                resultsString.append("                    <td class=\"parentCell\" style=\"" + tdStyleString + styleString + "\">" + rankChange +
                        "<span class=\"tooltip\">" + tooltipString + "</span></td>\n" +
                        "                <td></td>\n");
                for (TermId hpoId : result.rankMaxoScore().discoverableObservedHpoTermIds()) {
                    Integer ct1 = hpoTermIdRepCtsMap.get(omimId).get(hpoId);
                    double opacity1 = (ct1 == null) ? 0 :
                            (result.rankMaxoScore().discoverableObservedDescendantHpoTermIds().contains(hpoId) ? 0.5 : 1);
                    String hpoDivStyleString = divStyleString + "background: rgba(160, 32, 240, " + opacity1 + ")";
                            resultsString.append("                        <td style=\"" + tdStyleString + "\">" +
                                    "                       <div style=\"" + hpoDivStyleString + "\"></div></td>\n");
                }

                resultsString.append("            </tr>\n");
            }

            resultsString.append("        </tbody>\n" +
                    "    </table>");
            resultsString.append("</div>");

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
