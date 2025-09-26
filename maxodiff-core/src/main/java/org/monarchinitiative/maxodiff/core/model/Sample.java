package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

/**
 * Sample is input for maxodiff Differential Diagnosis refinement.
 */
public interface Sample {

    static Sample of(
            String id,
            Collection<TermId> observedHpoTermIds,
            Collection<TermId> excludedHpoTermIds
    ) {
        return new SimpleSample(id, observedHpoTermIds, excludedHpoTermIds);
    }

    String id();

    Collection<TermId> observedHpoTermIds();

    Collection<TermId> excludedHpoTermIds();

}
