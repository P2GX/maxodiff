package org.p2gx.maxodiff.core.analysis;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.model.TermPair;
import org.p2gx.maxodiff.core.phenomizer.IcMicaData;

import java.util.*;

public class HTMLFrequencyMap {
    private final HpoDiseases diseases;
    private final Map<TermPair, Double> icMicaData;
    /** Largest finite IC of any most informative common ancestor (MICA); computed once when the data was loaded. */
    private final double maxMica;

    public HTMLFrequencyMap(
           MdContext context
    ) {
        this.diseases = context.resources().hpoDiseases();
        this.icMicaData = context.resources().icMicaData().icMicaDict();
        this.maxMica = context.resources().icMicaData().maxIc();
    }

    public HTMLFrequencyMap(
            HpoDiseases hpoDiseases,
            IcMicaData icMicaData
    ) {
        this.diseases = hpoDiseases;
        this.icMicaData = icMicaData.icMicaDict();
        this.maxMica = icMicaData.maxIc();
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
        return maxMica;
    }


}
