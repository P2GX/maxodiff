package org.monarchinitiative.maxodiff.core.analysis.impl;

import org.monarchinitiative.maxodiff.core.analysis.DiseaseTermCount;
import org.monarchinitiative.maxodiff.core.analysis.HTMLFrequencyMap;
import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.phenol.annotations.base.Ratio;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class DiseaseTermCountImpl implements DiseaseTermCount {

    private static final DiseaseTermCountImpl EMPTY = new DiseaseTermCountImpl(List.of(), Map.of());

    public static DiseaseTermCountImpl empty() {
        return EMPTY;
    }

    /** A list of all diseases in the HPO Annotation file (usually restricted to OMIM entries). */
    private final List<HpoDisease> diseaseList;
    /** Key is an HPO term Id, and value is a list of {@link HpoFrequency} objects for that term.  Each object is an OMIM id*/
    private final Map<TermId, List<HpoFrequency>> hpoTermCounts;

    public DiseaseTermCountImpl(
            List<HpoDisease> diseaseList,
            Map<TermId, List<HpoFrequency>> hpoTermCounts) {
        this.diseaseList = Objects.requireNonNull(diseaseList);
        this.hpoTermCounts = hpoTermCounts;
    }

    /**
     * Default implementation that constructs a {@link DiseaseTermCount} from a list of {@link HpoDisease}s.
     *
     * @param diseaseList list of diseases to include
     * @return a {@link DiseaseTermCountImpl} summarizing term frequencies
     */
    public static DiseaseTermCountImpl defaultCount(List<HpoDisease> diseaseList) {
        Map<TermId, List<HpoFrequency>> hpoTermCounts = new HashMap<>();
        for (HpoDisease disease : diseaseList) {
            TermId omimId = disease.id();
            for (TermId hpoId : disease.annotationTermIdList()) {
                List<HpoFrequency> freqRecords = hpoTermCounts.computeIfAbsent(hpoId, id -> new ArrayList<>());
                float freq = disease.getFrequencyOfTermInDisease(hpoId).map(Ratio::frequency).orElse(1f);
                freqRecords.add(new HpoFrequency(omimId.toString(), hpoId.toString(), 1, freq, 0f));
            }
        }
        return new DiseaseTermCountImpl(diseaseList, hpoTermCounts);
    }


    public int nDiseases() { return diseaseList.size(); }

    public List<HpoDisease> hpoDiseases() { return diseaseList; }

    public int nHpoTerms() { return hpoTermCounts.size(); }

    public Map<TermId, List<HpoFrequency>> hpoTermCounts() { return hpoTermCounts; }

}
