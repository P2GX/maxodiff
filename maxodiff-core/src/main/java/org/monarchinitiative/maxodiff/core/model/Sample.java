package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

/**
 * Sample is input for maxodiff Differential Diagnosis refinement.
 * Sample includes differential diagnosis results from some source.
 * We don't expect to get differential diagnosis results for all possible diseases, e.g. the entire OMIM corpus.
 * We don't expect the collection of differential diagnoses to be in any particular order.
 */
public interface Sample {

    static Sample of(
            String id,
            Collection<TermId> presentHpoTermIds,
            Collection<TermId> excludedHpoTermIds,
            Collection<DifferentialDiagnosisModel> differentialDiagnoses
    ) {
        return new SimpleSample(id, presentHpoTermIds, excludedHpoTermIds, differentialDiagnoses);
    }

    String id();

    Collection<TermId> presentHpoTermIds();

    Collection<TermId> excludedHpoTermIds();

    Collection<DifferentialDiagnosisModel> differentialDiagnoses();
}
