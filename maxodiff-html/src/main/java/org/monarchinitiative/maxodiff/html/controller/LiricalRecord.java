package org.monarchinitiative.maxodiff.html.controller;

import org.monarchinitiative.lirical.core.model.TranscriptDatabase;

import java.nio.file.Path;

@Deprecated(forRemoval = true)
public record LiricalRecord(String genomeBuild, TranscriptDatabase transcriptDatabase, Float pathogenicityThreshold,
                            Double defaultVariantBackgroundFrequency, boolean strict, boolean globalAnalysisMode,
                            Path vcfPath) {
}
