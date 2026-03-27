package org.p2gx.maxodiff.html.results;

public class MaxodiffHtml {

    private final String sampleResultsTitle;
    private final String samplePresentHpoIds;
    private final String sampleExcludedHpoIds;
    private final int nDiseases;
    private final int nSimulations;
    private final String resultsString;

    public MaxodiffHtml(String sampleResultsTitle,
                        String samplePresentHpoIds,
                        String sampleExcludedHpoIds,
                        int nDiseases,
                        int nSimulations,
                        String resultsString) {
        this.sampleResultsTitle = sampleResultsTitle;
        this.samplePresentHpoIds = samplePresentHpoIds;
        this.sampleExcludedHpoIds = sampleExcludedHpoIds;
        this.nDiseases = nDiseases;
        this.nSimulations = nSimulations;
        this.resultsString = resultsString;
    }

    public String getSampleResultsTitle() {
        return sampleResultsTitle;
    }

    public String getSamplePresentHpoIds() {
        return samplePresentHpoIds;
    }

    public String getSampleExcludedHpoIds() {
        return sampleExcludedHpoIds;
    }

    public int getnDiseases() {
        return nDiseases;
    }

    public int getnSimulations() {
        return nSimulations;
    }

    public String getResultsString() {
        return resultsString;
    }
}
