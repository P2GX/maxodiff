package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;


public record LiricalResultsFileRecord(TermId omimId, String omimLabel, Double posttestProbability, Double likelihoodRatio) {}

