package org.p2gx.maxodiff.core.analysis;

import org.monarchinitiative.phenol.annotations.base.Ratio;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.phenomizer.IcMicaData;

import java.util.ArrayList;
import java.util.List;

/** Builds the table of HPO term frequencies and MICA values for all diseases. */
public final class HpoFrequencies {

    private HpoFrequencies() {
    }

    /**
     * @return one record for every (disease, annotated HPO term) pair: the frequency of the term in the disease,
     *         and the largest MICA between the term and the disease's observed annotations
     */
    public static List<HpoFrequency> compute(HpoDiseases hpoDiseases, IcMicaData icMicaData) {
        List<HpoFrequency> hpoFrequencies = new ArrayList<>();
        for (HpoDisease disease : hpoDiseases) {
            TermId diseaseId = disease.id();
            // built once per disease, not once per (disease, term) pair
            List<TermId> presentIds = disease.presentAnnotationsStream().map(HpoDiseaseAnnotation::id).toList();
            for (TermId hpoId : disease.annotationTermIdList()) {
                float freq = disease.getFrequencyOfTermInDisease(hpoId).map(Ratio::frequency).orElse(1f);
                float mica = (float) icMicaData.getMaxIc(hpoId, presentIds);
                hpoFrequencies.add(new HpoFrequency(diseaseId, hpoId, freq, mica));
            }
        }
        return hpoFrequencies;
    }
}