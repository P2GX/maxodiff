package org.monarchinitiative.maxodiff.core.analysis;

import java.util.List;

public record MdMetadata(String ppktId, int nDiseases, int nRepetitions,
                         List<SimpleTerm> observedHpoTerms,
                         List<SimpleTerm> excludedHpoTerms) {
}
