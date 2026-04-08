package org.p2gx.maxodiff.core.io;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.p2gx.maxodiff.core.analysis.HTMLFrequencyMap;
import org.p2gx.maxodiff.core.analysis.MySimpleTerm;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import org.p2gx.maxodiff.core.phenomizer.IcMicaData;

import java.util.Map;
import java.util.Set;

public record MdResources(MinimalOntology hpo,
                          HpoDiseases hpoDiseases,
                          Map<MySimpleTerm, Set<MySimpleTerm>> maxoAnnotsMap,
                          Map<MySimpleTerm, Set<MySimpleTerm>> maxoToHpoMap,
                          IcMicaData icMicaData) {
}
