package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public record SamplePhenopacket(String id, List<TermId> presentHpoTermIds, List<TermId> excludedHpoTermIds,
                                List<TermId> diseaseIds) {

}
