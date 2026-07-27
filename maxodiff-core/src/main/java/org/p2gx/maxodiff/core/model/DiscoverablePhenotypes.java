package org.p2gx.maxodiff.core.model;

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
    private final Map<TermId, Set<TermId>> hpoToMaxoTermIdMap;
    private final Map<TermId, Set<TermId>> maxoToHpoTermIdMap;

    /**
     *
     * @param hpoDiseases HpoDisease object
     * @param hpoToMaxoTermIdMap Map of HPO term ids : Set of associated MAxO term ids created using maxo_diagnostic_annotations file.
     * @param maxoToHpoTermIdMap Map of MAxO term ids : Set of associated HPO term ids created using maxo_diagnostic_annotations file.
     */
    public DiscoverablePhenotypes(HpoDiseases hpoDiseases,
                                  Map<TermId, Set<TermId>> hpoToMaxoTermIdMap,
                                  Map<TermId, Set<TermId>> maxoToHpoTermIdMap) {
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
    public Set<TermId> getDiscoverablePhenotypeIds(PhenopacketData samplePhenopacket, TermId targetDiseaseId) throws PhenolRuntimeException {
        AssessablePhenotypes assessablePhenotypes = new AssessablePhenotypes(hpoDiseases);
        ExcludedPhenotypes assumedExcludedPhenotypes = new ExcludedPhenotypes(hpoToMaxoTermIdMap, maxoToHpoTermIdMap);
        Set<TermId> ascertainablePhenotypeIds = assessablePhenotypes.getAssessablePhenotypeIds(samplePhenopacket, targetDiseaseId);
        Set<TermId> excludedPhenotypeIds = assumedExcludedPhenotypes.getExcludedPhenotypeIds(samplePhenopacket);

        Set<TermId> discoverablePhenotypes = new HashSet<>(ascertainablePhenotypeIds);
        excludedPhenotypeIds.forEach(discoverablePhenotypes::remove);

        return discoverablePhenotypes;
    }
}
