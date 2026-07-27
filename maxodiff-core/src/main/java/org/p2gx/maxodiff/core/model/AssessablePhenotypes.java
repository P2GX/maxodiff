package org.p2gx.maxodiff.core.model;

import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class calculates the ascertainable phenotypes.
 * An <i>ascertainable phenotype</i> is defined as a phenotypic feature
 * that is associated with a disease but not currently mentioned in the
 * phenopacket. If we are considering a disease as a differential diagnosis,
 * then it is useful to know if such features are present (this would increase
 * our belief that the disease in question is the
 * correct diagnosis) or absent (this would decrease our belief).
 * <p>
 * Objects of this class offer the function getAscertainablePhenotypeIds
 * that get the ascertainable phenotypes for a given disease/phenopacket
 * </p>
 */
public class AssessablePhenotypes {
    /**
     * Reference to an object containing information about all diseases.
     */
    private final HpoDiseases hpoDiseases;

    /**
     * @param hpoDiseases HpoDisease object
     */
    public AssessablePhenotypes(HpoDiseases hpoDiseases) {
        this.hpoDiseases = hpoDiseases;
    }

    /**
     *
     * @param myPpkt Input phenopacket with present and excluded HPO terms
     * @param diseaseId TermId of the disease of interest
     * @return Ascertainable term Ids: HPO terms that are annotated to the disease, but are not present in the phenopacket.
     * @throws PhenolRuntimeException if that targetDiseaseId is not found.
     */
    public Set<TermId> getAssessablePhenotypeIds(
            PhenopacketData myPpkt,
            TermId diseaseId) throws PhenolRuntimeException {
        HpoDisease disease = hpoDiseases.diseaseById(diseaseId)
                .orElseThrow(() -> new PhenolRuntimeException("Could not find disease id " + diseaseId.getValue()));
        Set<TermId> allPkktTerms = myPpkt.observed().stream().map(SimpleTerm::tid).collect(Collectors.toSet());
        allPkktTerms.addAll(myPpkt.excluded().stream().map(SimpleTerm::tid).collect(Collectors.toSet()));
        // Ascertainable phenotypes include all terms not currently mentioned
        // in the phenopacket
        return disease.annotationTermIdList().stream()
                .filter(value -> !allPkktTerms.contains(value))
                .collect(Collectors.toSet());
    }


}
