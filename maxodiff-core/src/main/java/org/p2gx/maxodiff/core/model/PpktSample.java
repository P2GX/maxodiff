package org.p2gx.maxodiff.core.model;

import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import java.util.List;

public record PpktSample(String id,
                         List<SimpleTerm> observedHpoTerms,
                         List<SimpleTerm> excludedHpoTerms) {
}
