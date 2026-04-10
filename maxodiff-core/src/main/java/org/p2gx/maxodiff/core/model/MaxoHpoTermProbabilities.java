package org.p2gx.maxodiff.core.model;

import org.p2gx.maxodiff.core.analysis.MaxoHpoTermIdMaps;
import org.p2gx.maxodiff.core.analysis.MySimpleTerm;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

public class MaxoHpoTermProbabilities {

    private final HpoDiseases hpoDiseases;
    private final List<DifferentialDiagnosis> initialDiagnoses; //top K diagnoses only
    private final Map<String, Set<String>> maxoToHpoTermIdMap;
    private final DiscoverablePhenotypes discoverablePhenotypes;

    public MaxoHpoTermProbabilities(HpoDiseases hpoDiseases,
                                    Map<MySimpleTerm, Set<MySimpleTerm>> hpoToMaxoTermMap,
                                    List<DifferentialDiagnosis> initialDiagnoses) {
        this.hpoDiseases = hpoDiseases;
        this.initialDiagnoses = initialDiagnoses;
        Map<String, Set<String>> hpoToMaxoTermIdMap = MaxoHpoTermIdMaps.getHpoToMaxoTermIdMap(hpoToMaxoTermMap);
        this.maxoToHpoTermIdMap = MaxoHpoTermIdMaps.getMaxoToHpoTermIdMap(hpoToMaxoTermMap);
        this.discoverablePhenotypes = new DiscoverablePhenotypes(hpoDiseases, hpoToMaxoTermIdMap, maxoToHpoTermIdMap);
    }

    /**
     *
     * @param ppkt Input phenopacket with present and excluded HPO terms
     * @return Set of all discoverable phenotypes, i.e. potential phenotypes not including assumed excluded phenotypes,
     * for all K diseases in the differential diagnosis
     */
    public Set<String> getUnionOfDiscoverablePhenotypes(PpktSample ppkt) {
        Set<String> unionDiscoverablePhenotypes = new HashSet<>();

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
    public Set<String> getDiscoverableByMaxoHpoTerms(PpktSample ppkt, TermId maxoId, Map<String, Set<String>> maxoToHpoTermIdMap) {
        Set<String> maxoAssociatedHpoIds = maxoToHpoTermIdMap.get(maxoId.getValue());
        if (maxoAssociatedHpoIds != null) {
            Set<String> unionDiscoverablePhenotypes = getUnionOfDiscoverablePhenotypes(ppkt);
            maxoAssociatedHpoIds.retainAll(unionDiscoverablePhenotypes); //intersection
        } else {
            maxoAssociatedHpoIds = Set.of();
        }

        return maxoAssociatedHpoIds;

    }

    public int nDiseases() { return initialDiagnoses.size(); }

    public List<DifferentialDiagnosis> getInitialDiagnoses() { return initialDiagnoses; }

    public HpoDiseases getHpoDiseases() { return hpoDiseases; }

    public Map<String, Set<String>> getMaxoToHpoTermIdMap() {
        return maxoToHpoTermIdMap;
    }
}
