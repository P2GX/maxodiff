package org.monarchinitiative.maxodiff.lirical;

import org.monarchinitiative.lirical.core.model.TranscriptDatabase;

import java.nio.file.Path;

public record LiricalRecord(String genomeBuild, TranscriptDatabase transcriptDatabase, Float pathogenicityThreshold,
                            Double defaultVariantBackgroundFrequency, boolean strict, boolean globalAnalysisMode,
                            Path liricalDataDir, Path exomiserPath, Path vcfPath) {
}
