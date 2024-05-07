package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiseaseTermCountImpl implements DiseaseTermCount {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiseaseTermCountImpl.class);

    private static final DiseaseTermCountImpl EMPTY = new DiseaseTermCountImpl(List.of());

    static DiseaseTermCountImpl empty() {
        return EMPTY;
    }

    public record HpoFrequency(String omimId, String hpoId, Integer count, Float frequency) {}

    private final List<HpoDisease> diseaseList;

    private final Map<TermId, List<HpoFrequency>> hpoTermCounts;

    public DiseaseTermCountImpl(List<HpoDisease> diseaseList) {
        this.diseaseList = Objects.requireNonNull(diseaseList);
        this.hpoTermCounts = new HashMap<>();
        for (HpoDisease disease : diseaseList) {
            TermId omimId = disease.id();
            List<TermId> termIds = disease.annotationTermIdList();
            LOGGER.debug(disease.diseaseName() + ": " + termIds);
            for (TermId hpoId : termIds) {
                List<HpoFrequency> freqRecords = !this.hpoTermCounts.containsKey(hpoId) ? new ArrayList<>() : this.hpoTermCounts.get(hpoId);
                float hpoTermFreq = disease.getFrequencyOfTermInDisease(hpoId).orElse(null).frequency();
                freqRecords.add(new HpoFrequency(omimId.toString(), hpoId.toString(), 1, hpoTermFreq));
                this.hpoTermCounts.put(hpoId, freqRecords);
            }
        }
    }

    public int nDiseases() { return diseaseList.size(); }

    public List<HpoDisease> hpoDiseases() { return diseaseList; }

    public int nHpoTerms() { return hpoTermCounts.size(); }

    public Map<TermId, List<HpoFrequency>> hpoTermCounts() { return hpoTermCounts; }

}
