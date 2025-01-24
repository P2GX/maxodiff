package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

/**
 * This class calculates the ascertainable phenotypes.
 * An \textit{ascertainable phenotype} is defined as a phenotypic feature that is associated with a disease but not
 * currently mentioned in the phenopacket. If we are considering a disease as a differential diagnosis, then it is
 * useful to know if such features are present (this would increase our belief that the disease in question is the
 * correct diagnosis) or absent (this would decrease our belief).
 */
public class AscertainablePhenotypes {
    /**
     * Reference to an object containing information about all diseases.
     */
    private final HpoDiseases hpoDiseases;

    /**
     *
     * @param hpoDiseases HpoDisease object
     */
    public AscertainablePhenotypes(HpoDiseases hpoDiseases) {
        this.hpoDiseases = hpoDiseases;
    }

    /**
     *
     * @param myPpkt Input phenopacket with present and excluded HPO terms
     * @param targetDiseaseId TermId of the disease of interest
     * @return Ascertainable term Ids: HPO terms that are annotated to the disease, but are not present in the phenopacket.
     * @throws PhenolRuntimeException if that targetDiseaseId is not found.
     */
    //TODO: use Sample instead of SamplePhenopacket and pass List of diseaseIds separately
    public Set<TermId> getAscertainablePhenotypeIds(Sample myPpkt, TermId targetDiseaseId) throws PhenolRuntimeException {
        Set<TermId> existingTerms = new HashSet<>(myPpkt.presentHpoTermIds());
        existingTerms.addAll(myPpkt.excludedHpoTermIds());
        Optional<HpoDisease> opt = hpoDiseases.diseaseById(targetDiseaseId);
        if (opt.isEmpty()) {
            throw new PhenolRuntimeException("Could not find disease id " + targetDiseaseId.getValue());
        }
        HpoDisease disease = opt.get();
        // Here, we do not care about present or absent. We regard all term annotations as
        // potentially relevant and worthy to be ascertained by a MAxO-annotated diagnostic method
        return new HashSet<>(disease.annotationTermIdList().stream()
                .filter(id -> !existingTerms.contains(id))
                .toList());
    }
}
