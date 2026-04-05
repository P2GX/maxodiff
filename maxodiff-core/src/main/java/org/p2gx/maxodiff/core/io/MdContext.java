package org.p2gx.maxodiff.core.io;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefiner;
import org.p2gx.maxodiff.core.analysis.refinement.DiffDiagRefinerImpl;
import org.p2gx.maxodiff.core.service.BiometadataService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


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

    public DiffDiagRefiner createRefiner() {
        Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap = resources.maxoAnnotsMap();
        MinimalOntology hpo = resources.hpo();
        HpoDiseases diseases = resources.hpoDiseases();


        Map<String, Set<String>> hpoToMaxoIdMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : maxoAnnotsMap.entrySet()) {
            String hpoId = entry.getKey().termId();
            Set<String> maxoIds = new HashSet<>();
            maxoAnnotsMap.get(entry.getKey()).forEach(t -> maxoIds.add(t.termId()));
            hpoToMaxoIdMap.put(hpoId, maxoIds);
        }
        return new DiffDiagRefinerImpl(diseases, hpoToMaxoIdMap, maxoAnnotsMap, hpo);
    }
}
