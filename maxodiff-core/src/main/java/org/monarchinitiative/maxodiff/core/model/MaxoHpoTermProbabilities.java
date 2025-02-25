package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.MaxoHpoTermIdMaps;
import org.monarchinitiative.phenol.annotations.base.Ratio;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class MaxoHpoTermProbabilities {

    private final HpoDiseases hpoDiseases;
    private final List<DifferentialDiagnosis> initialDiagnoses; //top K diagnoses only
    private final DiseaseModelProbability diseaseModelProbability;
//    private final Map<TermId, Set<TermId>> maxoToHpoTermIdMap;
    private final DiscoverablePhenotypes discoverablePhenotypes;

    public MaxoHpoTermProbabilities(HpoDiseases hpoDiseases, Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap,
                                  List<DifferentialDiagnosis> initialDiagnoses, DiseaseModelProbability diseaseModelProbability) {
        this.hpoDiseases = hpoDiseases;
        this.initialDiagnoses = initialDiagnoses;
        this.diseaseModelProbability = diseaseModelProbability;
//        this.maxoToHpoTermIdMap = MaxoHpoTermIdMaps.getMaxoToHpoTermIdMap(hpoToMaxoTermMap);
        this.discoverablePhenotypes = new DiscoverablePhenotypes(hpoDiseases, hpoToMaxoTermMap);
    }

    /**
     *
     * @param ppkt Input phenopacket with present and excluded HPO terms
     * @return Set of all discoverable phenotypes, i.e. potential phenotypes not including assumed excluded phenotypes,
     * for all K diseases in the differential diagnosis
     */
    public Set<TermId> getUnionOfDiscoverablePhenotypes(Sample ppkt) {
        Set<TermId> unionDiscoverablePhenotypes = new HashSet<>();

        for (DifferentialDiagnosis diagnosis : initialDiagnoses) {
            unionDiscoverablePhenotypes.addAll(discoverablePhenotypes.getDiscoverablePhenotypeIds(
                    ppkt, diagnosis.diseaseId()));
        }

        return unionDiscoverablePhenotypes;

    }

    /**
     *
     * @param ppkt Input phenopacket with present and excluded HPO terms
     * @param maxoId Term Id for the MAxO term of interest
     * @return HPO terms discoverable by the MAxO term, i.e. the intersection of the HPO terms that can be ascertained by
     * that MAxO term and the union of discoverable phenotypes for the diseases
     */
    public Set<TermId> getDiscoverableByMaxoHpoTerms(Sample ppkt, TermId maxoId, Map<TermId, Set<TermId>> maxoToHpoTermIdMap) {
        Set<TermId> maxoAssociatedHpoIds = maxoToHpoTermIdMap.get(maxoId);
        if (maxoAssociatedHpoIds != null) {
            Set<TermId> unionDiscoverablePhenotypes = getUnionOfDiscoverablePhenotypes(ppkt);
            maxoAssociatedHpoIds.retainAll(unionDiscoverablePhenotypes); //intersection
        } else {
            maxoAssociatedHpoIds = Set.of();
        }

        return maxoAssociatedHpoIds;

    }

    /**
     *
     * @param hpoId HPO term of interest
     * @return Probability that the HPO term will be ascertained by a diagnostic procedure
     */
    public double calculateProbabilityOfMaxoTermRevealingPresenceOfHpoTerm(TermId hpoId) {
        double p = 0.;
        for (DifferentialDiagnosis diagnosis : initialDiagnoses) {
            double diseaseProbability = diseaseModelProbability.probability(diagnosis.diseaseId());
            Optional<HpoDisease> hpoDiseaseOpt = hpoDiseases.diseaseById(diagnosis.diseaseId());
            if (hpoDiseaseOpt.isPresent()) {
                HpoDisease hpoDisease = hpoDiseaseOpt.get();
                Optional<Ratio> frequencyOfTermInDiseaseOpt = hpoDisease.getFrequencyOfTermInDisease(hpoId);
                if (frequencyOfTermInDiseaseOpt.isPresent()) {
                    float termFrequencyInDisease = frequencyOfTermInDiseaseOpt.get().frequency();
                    p += termFrequencyInDisease * diseaseProbability;
                }
            }
        }

        return p;
    }

    public int nDiseases() { return initialDiagnoses.size(); }

    public List<DifferentialDiagnosis> getInitialDiagnoses() { return initialDiagnoses; }
}
