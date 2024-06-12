package org.monarchinitiative.maxodiff.core.service;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Map;
import java.util.Optional;

public class BiometadataServiceImpl implements BiometadataService {

    private final Map<String, String> maxoTermsMap;
    private final Map<TermId, String> hpoTermsMap;
    private final Map<TermId, String> diseaseTermsMap;

    public BiometadataServiceImpl(Map<String, String> maxoTermsMap,
                                  Map<TermId, String> hpoTermsMap,
                                  Map<TermId, String> diseaseTermsMap) {

        this.maxoTermsMap = maxoTermsMap;
        this.hpoTermsMap = hpoTermsMap;
        this.diseaseTermsMap = diseaseTermsMap;
    }


    @Override
    public Optional<String> hpoLabel(TermId termId) {
        return Optional.ofNullable(hpoTermsMap.get(termId));
    }

    @Override
    public Optional<String> maxoLabel(String curie) {
        return Optional.ofNullable(maxoTermsMap.get(curie));
    }

    @Override
    public Optional<String> diseaseLabel(TermId diseaseId) {
        return Optional.ofNullable(diseaseTermsMap.get(diseaseId));
    }
}
