package org.monarchinitiative.maxodiff.core.analysis;

/** The count is the number of times that the HPO term was observed in our simulations */
public record CountedHpoTerm(
        SimpleTerm hpoTerm,
        int count
) {
}
