package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.model.SamplePhenopacket;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.base.PhenolRuntimeException;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class ExcludedPhenotypes {

    private final HpoDiseases hpoDiseases;
    private final Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap;
    private final Map<TermId, Set<TermId>> hpoToMaxoTermIdMap;
    private final Map<TermId, Set<TermId>> maxoToHpoTermIdMap;

    public ExcludedPhenotypes(HpoDiseases hpoDiseases, Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap) {
        this.hpoDiseases = hpoDiseases;
        this.hpoToMaxoTermMap = hpoToMaxoTermMap;
        this.hpoToMaxoTermIdMap = getHpoToMaxoTermIdMap();
        this.maxoToHpoTermIdMap = getMaxoToHpoTermIdMap();
    }

    public Map<TermId, Set<TermId>> getHpoToMaxoTermIdMap() {
        Map<TermId, Set<TermId>> hpoToMaxoTermIdMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : hpoToMaxoTermMap.entrySet()) {
            TermId hpoId = entry.getKey().tid();
            Set<SimpleTerm> maxoTerms = entry.getValue();
            for (SimpleTerm maxoTerm : maxoTerms) {
                TermId maxoId = maxoTerm.tid();
                if (!hpoToMaxoTermIdMap.containsKey(hpoId)) {
                    hpoToMaxoTermIdMap.put(hpoId, new HashSet<>(Collections.singleton(maxoId)));
                } else {
                    Set<TermId> maxoIdSet = hpoToMaxoTermIdMap.get(hpoId);
                    maxoIdSet.add(maxoId);
                    hpoToMaxoTermIdMap.replace(hpoId, maxoIdSet);
                }
            }
        }
        return hpoToMaxoTermIdMap;
    }

    public Map<TermId, Set<TermId>> getMaxoToHpoTermIdMap() {
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : hpoToMaxoTermMap.entrySet()) {
            TermId hpoId = entry.getKey().tid();
            Set<SimpleTerm> maxoTerms = entry.getValue();
            for (SimpleTerm maxoTerm : maxoTerms) {
                TermId maxoId = maxoTerm.tid();
                if (!maxoToHpoTermIdMap.containsKey(maxoId)) {
                    maxoToHpoTermIdMap.put(maxoId, new HashSet<>(Collections.singleton(hpoId)));
                } else {
                    Set<TermId> hpoIdSet = maxoToHpoTermIdMap.get(maxoId);
                    hpoIdSet.add(hpoId);
                    maxoToHpoTermIdMap.replace(maxoId, hpoIdSet);
                }
            }
        }
        return maxoToHpoTermIdMap;
    }

    public Set<TermId> getExcludedPhenotypeIds(SamplePhenopacket samplePpkt, TermId targetDiseaseId) throws PhenolRuntimeException {

        Set<TermId> existingTerms = new HashSet<>(samplePpkt.presentHpoTermIds());
        existingTerms.addAll(samplePpkt.excludedHpoTermIds());

        Optional<HpoDisease> opt = hpoDiseases.diseaseById(targetDiseaseId);
        if (opt.isEmpty()) {
            throw new PhenolRuntimeException("Could not find disease id " + targetDiseaseId.getValue());
        }
        HpoDisease disease = opt.get();
        // CHECK -- DOES THIS GIVE US EVERYTHING
        // Here, we do not care about present or absent. We regard all term annotations as
        // potentially relevant and worthy to be ascertained by a Maxo-annotated diagnostic method
        Set<TermId> targetDiseaseAnnotatedHpoIds = new HashSet<>(disease.annotationTermIdList());


        //get set of maxo ids for each hpo term in phenopacket
        Set<TermId> associatedMaxoIds = new HashSet<>();
        for (TermId existingId : existingTerms) {
            if (hpoToMaxoTermIdMap.containsKey(existingId)) {
                associatedMaxoIds.addAll(hpoToMaxoTermIdMap.get(existingId));
            }
        }

        //then get intersection of hpo terms associated with each of these maxo terms
        Map<TermId, Set<TermId>> associatedMaxoHpoIdMap = new HashMap<>();
        associatedMaxoIds.forEach(tid -> associatedMaxoHpoIdMap.put(tid, maxoToHpoTermIdMap.get(tid)));

//        TermId firstMaxoId = associatedMaxoIds.stream().toList().getFirst();
//        Set<TermId> intersectionSet = new HashSet<>(associatedMaxoHpoIdMap.get(firstMaxoId));
//        for (int i = 1; i < associatedMaxoIds.size(); i++) {
//            TermId maxoId = associatedMaxoIds.stream().toList().get(i);
//            Set<TermId> set = new HashSet<>(associatedMaxoHpoIdMap.get(maxoId));
//            intersectionSet.retainAll(set);
//        }

        //TODO: incorporate HPOA diseases
        Set<TermId> intersectionSet = new HashSet<>();
        for (Map.Entry<TermId, Set<TermId>> assocMaxoHpoIdEntry : associatedMaxoHpoIdMap.entrySet()) {
            TermId maxoId = assocMaxoHpoIdEntry.getKey();
            Set<TermId> annotIntersectionSet = new HashSet<>(associatedMaxoHpoIdMap.get(maxoId));
//            System.out.println(maxoId + ": " + annotIntersectionSet);
            annotIntersectionSet.retainAll(targetDiseaseAnnotatedHpoIds);
            intersectionSet.addAll(annotIntersectionSet);
//            System.out.println(maxoId + " intersectSet " + intersectionSet);
        }

//        Set<TermId> maxoHpoIds = maxoToHpoTermIdMap.get(maxoTermId);
//        Set<TermId> maxoPpkHpoIds = new HashSet<>();
//        for (TermId id : maxoHpoIds) {
//            if (existingTerms.contains(id)) {
//                maxoPpkHpoIds.add(id);
//            }
//        }
//
//        Set<TermId> excludedTerms = new HashSet<>();
//        if (!maxoPpkHpoIds.isEmpty()) {
//            excludedTerms = maxoHpoIds;
//        }

//        System.out.println("existingTerms = " + existingTerms);
//        System.out.println("targetAnnotHpoIds = " + targetDiseaseAnnotatedHpoIds);
//        System.out.println("final intersectionSet = " + intersectionSet);

        return intersectionSet;
    }
}
