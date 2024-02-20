package org.monarchinitiative.maxodiff;

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

    private final List<HpoDisease> diseaseList;

    private final Map<TermId, List<Object>> hpoTermCounts;

    public DiseaseTermCountImpl(List<HpoDisease> diseaseList) {
        this.diseaseList = Objects.requireNonNull(diseaseList);
        this.hpoTermCounts = new HashMap<>();
        for (HpoDisease disease : diseaseList) {
            List<TermId> termIds = disease.annotationTermIdList();
            LOGGER.debug(disease.diseaseName() + ": " + termIds);
            for (TermId id : termIds) {
                List<Object> countFreqList;
                float termFreq = disease.getFrequencyOfTermInDisease(id).orElse(null).frequency();
                if (!this.hpoTermCounts.containsKey(id)) {
                    //Add list of term count and frequency to map
                    countFreqList = Arrays.asList(1, termFreq);
                    this.hpoTermCounts.put(id, countFreqList);
                } else {
                    //Replace list of term count and frequency in map
                    List<Object> list = this.hpoTermCounts.get(id);
                    int oldCount = (int) list.get(0);
                    list.set(0, oldCount + 1);
                    list.set(1, termFreq);
                    this.hpoTermCounts.replace(id, list);
                }
            }
        }
    }

    public int nDiseases() { return diseaseList.size(); }

    public List<HpoDisease> hpoDiseases() { return diseaseList; }

    public int nHpoTerms() { return hpoTermCounts.size(); }

    public Map<TermId, List<Object>> hpoTermCounts() { return hpoTermCounts; }

}
