package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.model.SamplePhenopacket;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class PotentialPhenotypes {

    private final HpoDiseases hpoDiseases;

    public PotentialPhenotypes(HpoDiseases hpoDiseases) {
        this.hpoDiseases = hpoDiseases;
    }

    public Set<TermId> getPotentialPhenotypeIds(SamplePhenopacket myPpkt, TermId targetDiseaseId) throws PhenolRuntimeException {
        Set<TermId> existingTerms = new HashSet<>(myPpkt.presentHpoTermIds());
        existingTerms.addAll(myPpkt.excludedHpoTermIds());
        Optional<HpoDisease> opt = hpoDiseases.diseaseById(targetDiseaseId);
        if (opt.isEmpty()) {
            throw new PhenolRuntimeException("Could not find disease id " + targetDiseaseId.getValue());
        }
        HpoDisease disease = opt.get();
        // CHECK -- DOES THIS GIVE US EVERYTHING
        // Here, we do not care about present or absent. We regard all term annotations as
        // potentially relevant and worthy to be ascertained by a Maxo-annotated diagnostic method
        Set<TermId> allIds = new HashSet<>(disease.annotationTermIdList());

        Set<TermId> potentialTerms = new HashSet<>();
        for (TermId id : allIds) {
            //TODO: what about inheritance?
            if (existingTerms.contains(id)) {
                continue;
            } else {
                potentialTerms.add(id);
            }
        }

        return potentialTerms;
    }
}
