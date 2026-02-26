package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.maxodiff.core.analysis.SimpleTerm;
import java.util.List;

public record PpktSample(String id,
                         List<SimpleTerm> observedHpoTerms,
                         List<SimpleTerm> excludedHpoTerms) {
}
