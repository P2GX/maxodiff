package org.monarchinitiative.maxodiff.html.config;

import org.monarchinitiative.lirical.core.model.GenomeBuild;
import org.monarchinitiative.lirical.core.model.TranscriptDatabase;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;

@ConfigurationProperties(prefix = "lirical")
public class LiricalProperties {

    private String dataDirectory = "data/lirical";
    private GenomeBuild genomeBuild = GenomeBuild.HG38;
    private TranscriptDatabase transcriptDatabase = TranscriptDatabase.REFSEQ;
    private float pathogenicityThreshold = .8f;
    private double defaultVariantBackgroundFrequency = .1;
    private boolean strict = false;
    private boolean globalMode = false;
    private String exomiserHg19Path;
    private String exomiserHg38Path;

    public String getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public GenomeBuild getGenomeBuild() {
        return genomeBuild;
    }

    public void setGenomeBuild(GenomeBuild genomeBuild) {
        this.genomeBuild = genomeBuild;
    }

    public TranscriptDatabase getTranscriptDatabase() {
        return transcriptDatabase;
    }

    public void setTranscriptDatabase(TranscriptDatabase transcriptDatabase) {
        this.transcriptDatabase = transcriptDatabase;
    }

    public float getPathogenicityThreshold() {
        return pathogenicityThreshold;
    }

    public void setPathogenicityThreshold(float pathogenicityThreshold) {
        this.pathogenicityThreshold = pathogenicityThreshold;
    }

    public double getDefaultVariantBackgroundFrequency() {
        return defaultVariantBackgroundFrequency;
    }

    public void setDefaultVariantBackgroundFrequency(double defaultVariantBackgroundFrequency) {
        this.defaultVariantBackgroundFrequency = defaultVariantBackgroundFrequency;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public boolean isGlobalMode() {
        return globalMode;
    }

    public void setGlobalMode(boolean globalMode) {
        this.globalMode = globalMode;
    }

    public String getExomiserHg19Path() {
        return exomiserHg19Path;
    }

    public void setExomiserHg19Path(String exomiserHg19Path) {
        this.exomiserHg19Path = exomiserHg19Path;
    }

    public String getExomiserHg38Path() {
        return exomiserHg38Path;
    }

    public void setExomiserHg38Path(String exomiserHg38Path) {
        this.exomiserHg38Path = exomiserHg38Path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LiricalProperties that = (LiricalProperties) o;
        return Objects.equals(dataDirectory, that.dataDirectory) && Float.compare(pathogenicityThreshold, that.pathogenicityThreshold) == 0 && Double.compare(defaultVariantBackgroundFrequency, that.defaultVariantBackgroundFrequency) == 0 && strict == that.strict && globalMode == that.globalMode && Objects.equals(genomeBuild, that.genomeBuild) && transcriptDatabase == that.transcriptDatabase && Objects.equals(exomiserHg19Path, that.exomiserHg19Path) && Objects.equals(exomiserHg38Path, that.exomiserHg38Path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataDirectory, genomeBuild, transcriptDatabase, pathogenicityThreshold, defaultVariantBackgroundFrequency, strict, globalMode, exomiserHg19Path, exomiserHg38Path);
    }

    @Override
    public String toString() {
        return "LiricalProperties{" +
                "dataDirectory='" + dataDirectory + '\'' +
                ", genomeBuild='" + genomeBuild + '\'' +
                ", transcriptDatabase=" + transcriptDatabase +
                ", pathogenicityThreshold=" + pathogenicityThreshold +
                ", defaultVariantBackgroundFrequency=" + defaultVariantBackgroundFrequency +
                ", strict=" + strict +
                ", globalMode=" + globalMode +
                ", exomiserHg19Path='" + exomiserHg19Path + '\'' +
                ", exomiserHg38Path='" + exomiserHg38Path + '\'' +
                '}';
    }
}
