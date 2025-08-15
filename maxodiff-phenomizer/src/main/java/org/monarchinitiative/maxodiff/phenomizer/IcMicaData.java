package org.monarchinitiative.maxodiff.phenomizer;

import org.monarchinitiative.phenol.ontology.similarity.TermPair;

import java.util.Map;

/**
 * A tuple with the IC MICA map and the corresponding metadata.
 *
 * @param icMicaDict map with IC<sub>MICA</sub> of a term pair.
 * @param metadata metadata with the source
 */
public record IcMicaData(Map<TermPair, Double> icMicaDict, IcMicaDictMetadata metadata) {
}
