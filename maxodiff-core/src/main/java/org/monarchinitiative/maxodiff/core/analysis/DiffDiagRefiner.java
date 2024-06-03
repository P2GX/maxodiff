package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.model.Sample;

/**
 * TODO: insert description
 */
public interface DiffDiagRefiner {

    RefinementResults run(Sample sample, RefinementOptions options);
}
