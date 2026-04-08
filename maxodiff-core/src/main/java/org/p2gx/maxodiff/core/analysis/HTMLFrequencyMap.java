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
    /** Maximum minimum informatic content of most informative common ancestor (MICA)
     * We calculate this dynamically but this is a fall back in case there is
     * an issue*/
    private final static double DEFAULT_MAX_MICA = 8.343077871169383;

    public HTMLFrequencyMap(
           MdContext context
    ) {
        this.diseases = context.resources().hpoDiseases();
        this.icMicaData = context.resources().icMicaData().icMicaDict();
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

    /**
     * @return maximum information content of any most informative common ancestor (MICA) in HPO annotation graph
     */
    public double maxMica() {
        return icMicaData.entrySet().stream()
                .filter(e -> !e.getValue().isInfinite())
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getValue)
                .orElse(DEFAULT_MAX_MICA);
    }

}
