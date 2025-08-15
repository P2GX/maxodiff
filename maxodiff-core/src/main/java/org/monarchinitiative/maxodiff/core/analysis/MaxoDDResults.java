package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record MaxoDDResults(Set<TermId> maxoDiscoverableHpoIds, Set<TermId> maxoDiscoverableExcludedHpoIds,
                            Set<TermId> maxoObservedDescendantHpoIds, List<DifferentialDiagnosis> maxoDifferentialDiagnoses) {
}
