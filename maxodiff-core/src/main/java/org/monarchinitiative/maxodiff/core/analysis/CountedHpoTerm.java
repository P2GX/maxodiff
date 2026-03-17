package org.monarchinitiative.maxodiff.core.analysis;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** The count is the number of times that the HPO term was observed in our simulations */
public record CountedHpoTerm(
        @JsonUnwrapped SimpleTerm hpoTerm,
        int count
) {
}
