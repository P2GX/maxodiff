package org.monarchinitiative.maxodiff.core;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;

public interface DiseaseTermCount {

    static DiseaseTermCount empty() {
        return DiseaseTermCountImpl.empty();
    }

    static DiseaseTermCount of(List<HpoDisease> diseaseList) {
        if (diseaseList.isEmpty())
            return DiseaseTermCountImpl.empty();
        return new DiseaseTermCountImpl(diseaseList);
    }

    int nDiseases();
    List<HpoDisease> hpoDiseases();
    int nHpoTerms();
    Map<TermId, List<Object>> hpoTermCounts();

}
