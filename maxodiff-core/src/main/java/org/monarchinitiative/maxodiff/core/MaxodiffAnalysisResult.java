package org.monarchinitiative.maxodiff.core;

import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.refinement.MaxodiffResult;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;

public record MaxodiffAnalysisResult(
        List<MaxodiffResult> results,
        Map<TermId, List<HpoFrequency>> hpoTermCounts
) {
}
