package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class calculates the discoverable phenotypes, i.e. potential phenotypes not including assumed
 * excluded phenotypes.
 */
public class DiscoverablePhenotypes {

    /**
     * Reference to an object containing information about all diseases.
     */
    private final HpoDiseases hpoDiseases;
    private final Map<String, Set<String>> hpoToMaxoTermIdMap;
    private final Map<String, Set<String>> maxoToHpoTermIdMap;

    /**
     *
     * @param hpoDiseases HpoDisease object
     * @param hpoToMaxoTermIdMap Map of HPO term ids : Set of associated MAxO term ids created using maxo_diagnostic_annotations file.
     * @param hpoToMaxoTermIdMap Map of HPO term ids : Set of associated MAxO term ids created using maxo_diagnostic_annotations file.
     */
    public DiscoverablePhenotypes(HpoDiseases hpoDiseases,
                                  Map<String, Set<String>> hpoToMaxoTermIdMap,
                                  Map<String, Set<String>> maxoToHpoTermIdMap) {
        this.hpoDiseases = hpoDiseases;
        this.hpoToMaxoTermIdMap = hpoToMaxoTermIdMap;
        this.maxoToHpoTermIdMap = maxoToHpoTermIdMap;
    }

    /**
     *
     * @param samplePhenopacket Input phenopacket with present and excluded HPO terms
     * @param targetDiseaseId TermId of the disease of interest
     * @return Set of discoverable phenotypes, i.e. potential phenotypes not including assumed excluded phenotypes.
     */
    public Set<String> getDiscoverablePhenotypeIds(PpktSample samplePhenopacket, TermId targetDiseaseId) throws PhenolRuntimeException {
        AscertainablePhenotypes ascertainablePhenotypes = new AscertainablePhenotypes(hpoDiseases);
        ExcludedPhenotypes excludedPhenotypes = new ExcludedPhenotypes(hpoToMaxoTermIdMap, maxoToHpoTermIdMap);
        Set<String> ascertainablePhenotypeIds = ascertainablePhenotypes.getAscertainablePhenotypeIds(samplePhenopacket, targetDiseaseId);
        Set<String> excludedPhenotypeIds = excludedPhenotypes.getExcludedPhenotypes(samplePhenopacket);

        Set<String> discoverablePhenotypes = new HashSet<>(ascertainablePhenotypeIds);
        excludedPhenotypeIds.forEach(discoverablePhenotypes::remove);

        return discoverablePhenotypes;
    }
}
