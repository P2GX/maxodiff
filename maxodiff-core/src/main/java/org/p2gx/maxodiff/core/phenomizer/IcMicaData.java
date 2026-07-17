package org.p2gx.maxodiff.core.phenomizer;

import org.monarchinitiative.phenol.ontology.data.TermId;
import org.p2gx.maxodiff.core.model.TermPair;

import java.util.List;
import java.util.Map;

/**
 * A tuple with the IC MICA (maximally informative common ancestor) map and the corresponding metadata.
 *
 * @param icMicaDict map with IC<sub>MICA</sub> of a term pair.
 * @param metadata metadata with the source
 */
public record IcMicaData(Map<TermPair, Double> icMicaDict, IcMicaDictMetadata metadata) {

    /**
     * Get the maximum MICA of a particular HPO term with a list of terms (e.g., all terms from a disease)
     * @param tid The HPO term of focus
     * @param annotatedTids A list of HPO term ids (e.g., from a disease)
     * @return The information content of the MICA (maximally informative common ancestor)
     */
    public double getMaxIc(TermId tid, List<TermId> annotatedTids) {
        double maxIc = 0;
        for (TermId annotatedTid : annotatedTids) {
            TermPair pair = TermPair.symmetric(tid, annotatedTid);
            double ic = icMicaDict.getOrDefault(pair, 0.0);
            if (ic > maxIc) {
                maxIc = ic;
            }
        }
        return maxIc;
    }

}
