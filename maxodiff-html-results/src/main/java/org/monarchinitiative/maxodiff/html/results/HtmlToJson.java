package org.monarchinitiative.maxodiff.html.results;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.monarchinitiative.maxodiff.html.results.maxoDisease.MaxoDiseaseHTML;
import org.monarchinitiative.maxodiff.html.results.maxoHpo.MaxoHtmlResult;

import java.io.IOException;

public class HtmlToJson {

    public static String convertHtmlToJson(MaxodiffHtml maxodiffHtml) {
        String jsonString = "";
        try {
            // Convert the Java object to a JSON string using Jackson
            ObjectMapper objectMapper = new ObjectMapper();
            jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(maxodiffHtml);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonString;
    }

    public static String convertDiseaseHtmlToJson(MaxoDiseaseHTML maxoDiseaseHtml) {
        String jsonString = "";
        try {
            // Convert the Java object to a JSON string using Jackson
            ObjectMapper objectMapper = new ObjectMapper();
            jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(maxoDiseaseHtml);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonString;
    }

    public static String convertHtmlResultsToJson(MaxoHtmlResult maxoHtmlResult) {
        String jsonString = "";
        try {
            // Convert the Java object to a JSON string using Jackson
            ObjectMapper objectMapper = new ObjectMapper();
            jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(maxoHtmlResult);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonString;
    }

}
