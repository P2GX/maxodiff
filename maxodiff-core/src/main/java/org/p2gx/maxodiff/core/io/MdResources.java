package org.p2gx.maxodiff.core.io;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.analysis.HpoFrequency;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import org.p2gx.maxodiff.core.phenomizer.IcMicaData;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record MdResources(MinimalOntology hpo,
                          HpoDiseases hpoDiseases,
                          Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap,
                          Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoMap,
                          IcMicaData icMicaData,
                          Map<TermId, Double> termToIcMap,
                          List<HpoFrequency> hpoFrequencies) {

    public MdResources {
        hpoFrequencies = hpoFrequencies == null ? null : List.copyOf(hpoFrequencies); // read-only, shared by all requests
    }

    /** For contexts without IC data, which have no frequency table. */
    public MdResources(MinimalOntology hpo, HpoDiseases hpoDiseases,
                       Map<SimpleTerm, Set<SimpleTerm>> maxoAnnotsMap,
                       Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoMap,
                       IcMicaData icMicaData, Map<TermId, Double> termToIcMap) {
        this(hpo, hpoDiseases, maxoAnnotsMap, maxoToHpoMap, icMicaData, termToIcMap, null);
    }
}
