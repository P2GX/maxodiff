package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

/**
 * Sample is input for maxodiff Differential Diagnosis refinement.
 */
public interface Sample {

    static Sample of(
            String id,
            Collection<TermId> presentHpoTermIds,
            Collection<TermId> excludedHpoTermIds
    ) {
        return new SimpleSample(id, presentHpoTermIds, excludedHpoTermIds);
    }

    String id();

    Collection<TermId> presentHpoTermIds();

    Collection<TermId> excludedHpoTermIds();

}
