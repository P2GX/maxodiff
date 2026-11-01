package org.p2gx.maxodiff.core.io;

import org.monarchinitiative.phenol.annotations.base.Ratio;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.HTMLFrequencyMap;
import org.p2gx.maxodiff.core.analysis.HpoFrequency;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefinerImpl;
import org.p2gx.maxodiff.core.service.BiometadataService;

import java.util.*;


public record MdContext(MdResources resources,
                        MdParams params,
                        BiometadataService biometadataService) {

    /**
     * Creates a new context with new parameters.
     */
    public MdContext withParams(MdParams newParams) {
        return new MdContext(resources, newParams, biometadataService);
    }

    public MdContext updateContext(int nRepetitions, int nDiseases) {
        MdParams params = new MdParams(nRepetitions, nDiseases);
        return new MdContext(resources, params, biometadataService);
    }

    public List<HpoFrequency> createHpoFrequencies() {
        List<HpoFrequency> hpoFrequencies = resources.hpoFrequencies();
        if (hpoFrequencies == null) {
            throw new IllegalStateException(
                    "HPO frequencies are only available for a context built with IC data (buildICData = true)");
        }
        return hpoFrequencies;
    }

    public DiffDiagRefiner createRefiner() {
        return new DiffDiagRefinerImpl(this);
    }

    @Override
    public String toString() {
        int nHpo = this.resources.hpo().allTermIdCount();
        int nMaxo = this.resources.maxoAnnotsMap().size();
        int nDiseases = this.resources.hpoDiseases().size();
        int nDxMaxo = this.resources.maxoAnnotsMap()
                .values()
                .stream().mapToInt(Set::size).sum();
        return String.format("HPO Terms: %s; Diagnostic MAxO terms: %d; diseases: %d; diagnostic MAXO annotations: %d",
                nHpo, nMaxo, nDiseases, nDxMaxo);
    }
}
