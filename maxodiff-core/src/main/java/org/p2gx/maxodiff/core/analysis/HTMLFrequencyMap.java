package org.p2gx.maxodiff.core.analysis;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;
import org.p2gx.maxodiff.core.io.MdContext;

import java.util.*;

public class HTMLFrequencyMap {
    private final HpoDiseases diseases;
    private final Map<TermPair, Double> icMicaData;

    public HTMLFrequencyMap(
            HpoDiseases diseases,
            Map<TermPair, Double> icMicaData
    ) {
        this.diseases = diseases;
        this.icMicaData = icMicaData;
    }

    public HTMLFrequencyMap(
           MdContext context
    ) {
        this.diseases = context.resources().hpoDiseases();
        this.icMicaData = context.resources().icMicaData().icMicaDict();
    }


    /**
     * Retrieve a flattened list of {@link HpoFrequency} records from the provided map.
     *
     * <p>Each {@link HpoFrequency} has an OMIM id, an HPO id, and a frequency..</p>
     *
     * @param hpoTermCounts a map where each key is an {@link TermId} and the value is a list of
     *                      {@link HpoFrequency} objects associated with that term
     * @return a combined list of all {@link HpoFrequency} objects across all HPO terms
     */
    public static List<HpoFrequency> getHpoFrequencies(Map<TermId, List<HpoFrequency>> hpoTermCounts) {
        return hpoTermCounts.values().stream()
                .flatMap(List::stream)
                .toList();
    }




    /**
     * @param hpoId target HPO term
     * @param diseaseId target OMIM disease
     * @return maximum MICA for the HPO term and any of the disease observed HPO terms
     */
    public float micaForDisease(TermId hpoId, TermId diseaseId) {
        Optional<HpoDisease> opt = this.diseases.diseaseById(diseaseId);
        if (opt.isEmpty()) {
            return 0f;
        }
        HpoDisease disease = opt.get();
        List<TermId> diseaseHpoTermIds = disease.presentAnnotationsStream()
                .map(HpoDiseaseAnnotation::id)
                .toList();
        float mica = 0f;
        for (TermId tid : diseaseHpoTermIds) {
            TermPair tp = TermPair.symmetric(tid, hpoId);
            double m = icMicaData.getOrDefault(tp, 0d);
            if (m > mica) mica = (float) m;
        }
        return mica;
    }


}
