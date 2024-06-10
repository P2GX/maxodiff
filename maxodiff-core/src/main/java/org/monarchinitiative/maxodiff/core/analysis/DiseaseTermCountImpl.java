package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

class DiseaseTermCountImpl implements DiseaseTermCount {

    private static final DiseaseTermCountImpl EMPTY = new DiseaseTermCountImpl(List.of(), Map.of());

    static DiseaseTermCountImpl empty() {
        return EMPTY;
    }

    private final List<HpoDisease> diseaseList;

    private final Map<TermId, List<HpoFrequency>> hpoTermCounts;

    DiseaseTermCountImpl(List<HpoDisease> diseaseList, Map<TermId, List<HpoFrequency>> hpoTermCounts) {
        this.diseaseList = Objects.requireNonNull(diseaseList);
        this.hpoTermCounts = hpoTermCounts;
    }

    public int nDiseases() { return diseaseList.size(); }

    public List<HpoDisease> hpoDiseases() { return diseaseList; }

    public int nHpoTerms() { return hpoTermCounts.size(); }

    public Map<TermId, List<HpoFrequency>> hpoTermCounts() { return hpoTermCounts; }

}
