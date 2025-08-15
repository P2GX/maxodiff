package org.monarchinitiative.maxodiff.core.service;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Optional;

public interface BiometadataService {

    Optional<String> hpoLabel(TermId termId);
    Optional<String> maxoLabel(String curie);
    Optional<String> diseaseLabel(TermId diseaseId);
}
