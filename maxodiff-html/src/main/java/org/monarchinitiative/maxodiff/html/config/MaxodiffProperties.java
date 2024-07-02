package org.monarchinitiative.maxodiff.html.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(prefix = "maxodiff")
public class MaxodiffProperties {

    private String dataDirectory;
    // TODO: is the scope of the following properties truly the entire application?
    private int nDiseases = 20;
    private double weight = 0.5;
    private int nMaxoResults = 10;

    public String getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public int getnDiseases() {
        return nDiseases;
    }

    public void setnDiseases(int nDiseases) {
        this.nDiseases = nDiseases;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getnMaxoResults() {
        return nMaxoResults;
    }

    public void setnMaxoResults(int nMaxoResults) {
        this.nMaxoResults = nMaxoResults;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MaxodiffProperties that = (MaxodiffProperties) o;
        return nDiseases == that.nDiseases && Double.compare(weight, that.weight) == 0 && nMaxoResults == that.nMaxoResults && Objects.equals(dataDirectory, that.dataDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataDirectory, nDiseases, weight, nMaxoResults);
    }

    @Override
    public String toString() {
        return "MaxodiffProperties{" +
                "dataDirectory='" + dataDirectory + '\'' +
                ", nDiseases=" + nDiseases +
                ", weight=" + weight +
                ", nMaxoResults=" + nMaxoResults +
                '}';
    }
}
