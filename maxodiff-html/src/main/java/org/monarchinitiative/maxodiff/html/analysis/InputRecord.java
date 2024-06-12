package org.monarchinitiative.maxodiff.html.analysis;

import org.monarchinitiative.lirical.core.analysis.AnalysisResults;

import java.nio.file.Path;


public record InputRecord(AnalysisResults liricalResults, Path phenopacketPath) {}

