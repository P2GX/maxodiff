package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
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
    private final Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap;

    /**
     *
     * @param hpoDiseases HpoDisease object
     */
    public DiscoverablePhenotypes(HpoDiseases hpoDiseases, Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap) {
        this.hpoDiseases = hpoDiseases;
        this.hpoToMaxoTermMap = hpoToMaxoTermMap;
    }

    /**
     *
     * @param samplePhenopacket Input phenopacket with present and excluded HPO terms
     * @param targetDiseaseId TermId of the disease of interest
     * @return Set of discoverable phenotypes, i.e. potential phenotypes not including assumed excluded phenotypes.
     */
    public Set<TermId> getDiscoverablePhenotypeIds(Sample samplePhenopacket, TermId targetDiseaseId) throws PhenolRuntimeException {
        AscertainablePhenotypes ascertainablePhenotypes = new AscertainablePhenotypes(hpoDiseases);
        ExcludedPhenotypes excludedPhenotypes = new ExcludedPhenotypes(hpoToMaxoTermMap);
        Set<TermId> ascertainablePhenotypeIds = ascertainablePhenotypes.getAscertainablePhenotypeIds(samplePhenopacket, targetDiseaseId);
        Set<TermId> excludedPhenotypeIds = excludedPhenotypes.getExcludedPhenotypes(samplePhenopacket);

        Set<TermId> discoverablePhenotypes = new HashSet<>(ascertainablePhenotypeIds);
        excludedPhenotypeIds.forEach(discoverablePhenotypes::remove);

        return discoverablePhenotypes;
    }
}
