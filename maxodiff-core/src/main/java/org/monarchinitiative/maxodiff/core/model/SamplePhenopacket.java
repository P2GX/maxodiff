package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record SamplePhenopacket(String id, List<TermId> presentHpoTermIds, List<TermId> excludedHpoTermIds,
                                List<TermId> diseaseIds) {

}
