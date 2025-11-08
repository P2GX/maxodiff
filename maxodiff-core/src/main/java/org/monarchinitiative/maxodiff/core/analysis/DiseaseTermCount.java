package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.analysis.impl.DiseaseTermCountImpl;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import java.util.List;
import java.util.Map;

/**
 * {@code DiseaseTermCount} provides summary statistics about the frequency of
 * Human Phenotype Ontology (HPO) terms across a set of {@link HpoDisease} objects.
 */
public interface DiseaseTermCount {
    int nDiseases();
    List<HpoDisease> hpoDiseases();
    int nHpoTerms();
    Map<TermId, List<HpoFrequency>> hpoTermCounts();

    /** Return an empty {@link DiseaseTermCount}. */
    static DiseaseTermCount empty() {
        return DiseaseTermCountImpl.empty();
    }

    /**
     * Create a {@link DiseaseTermCount} from a list of {@link HpoDisease} objects.
     * The implementation delegates to {@link DiseaseTermCountImpl#defaultCount(List)}.
     *
     * @param diseaseList list of diseases
     * @return a populated {@link DiseaseTermCount} instance
     */
    static DiseaseTermCount of(List<HpoDisease> diseaseList) {
        if (diseaseList == null || diseaseList.isEmpty())
            return DiseaseTermCountImpl.empty();
        return DiseaseTermCountImpl.defaultCount(diseaseList);
    }
}
