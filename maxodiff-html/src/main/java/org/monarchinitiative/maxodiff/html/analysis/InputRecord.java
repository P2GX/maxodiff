package org.monarchinitiative.maxodiff.html.analysis;

import org.monarchinitiative.lirical.core.analysis.AnalysisResults;
import org.monarchinitiative.maxodiff.core.analysis.MaxoTermMap;

import javax.validation.constraints.NotNull;
import java.nio.file.Path;


public record InputRecord(Path maxodiffDir, MaxoTermMap maxoTermMap, AnalysisResults liricalResults,
                          Path phenopacketPath) {}

