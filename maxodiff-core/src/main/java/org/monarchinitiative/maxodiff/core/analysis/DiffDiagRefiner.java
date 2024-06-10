package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;

import java.util.Collection;

/**
 * TODO: insert description
 * Differential diagnosis results come from some source.
 * We don't expect to get differential diagnosis results for all possible diseases, e.g. the entire OMIM corpus.
 * We don't expect the collection of differential diagnoses to be in any particular order.
 * The MAxO terms are returned in unspecified order.
 */
public interface DiffDiagRefiner {

    RefinementResults run(
            Sample sample,
            Collection<DifferentialDiagnosis> diagnoses,
            RefinementOptions options
    );
}
