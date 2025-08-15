package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.analysis.impl.DiseaseTermCountImpl;
import org.monarchinitiative.phenol.annotations.base.Ratio;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface DiseaseTermCount {

    static DiseaseTermCount empty() {
        return DiseaseTermCountImpl.empty();
    }

    static DiseaseTermCount of(List<HpoDisease> diseaseList) {
        if (diseaseList.isEmpty())
            return DiseaseTermCountImpl.empty();
        return makeDiseaseTermCount(diseaseList);
    }

    private static DiseaseTermCount makeDiseaseTermCount(List<HpoDisease> diseaseList) {
        Map<TermId, List<HpoFrequency>> hpoTermCounts = new HashMap<>();
        for (HpoDisease disease : diseaseList) {
            TermId omimId = disease.id();
            List<TermId> hpoIds = disease.annotationTermIdList();

            for (TermId hpoId : hpoIds) {
                List<HpoFrequency> freqRecords = hpoTermCounts.computeIfAbsent(hpoId, id -> new ArrayList<>());
                Float hpoTermFreq = disease.getFrequencyOfTermInDisease(hpoId).map(Ratio::frequency).orElse(null);
                freqRecords.add(new HpoFrequency(omimId.toString(), hpoId.toString(), 1, hpoTermFreq));
                hpoTermCounts.put(hpoId, freqRecords);
            }
        }
        return new DiseaseTermCountImpl(diseaseList, hpoTermCounts);
    }

    int nDiseases();
    List<HpoDisease> hpoDiseases();
    int nHpoTerms();
    Map<TermId, List<HpoFrequency>> hpoTermCounts();

}
