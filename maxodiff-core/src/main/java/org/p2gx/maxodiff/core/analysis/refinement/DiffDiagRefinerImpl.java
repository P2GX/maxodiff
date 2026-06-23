package org.p2gx.maxodiff.core.analysis.refinement;


import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResultSingleDisease;
import org.p2gx.maxodiff.core.diffdg.DDxEngine;

import org.p2gx.maxodiff.core.io.MdContext;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;

import org.p2gx.maxodiff.core.analysis.HpoFrequency;
import org.p2gx.maxodiff.core.model.*;


import java.util.*;
import java.util.stream.Collectors;

public class DiffDiagRefinerImpl implements DiffDiagRefiner {

    private final HpoDiseases hpoDiseases;
    private final Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoTermMap;
    private final Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap;
    private final MinimalOntology hpo;

    private MdContext context;

    public DiffDiagRefinerImpl(HpoDiseases hpoDiseases,
                               Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoTermMap,
                               Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap,
                               MinimalOntology hpo) {
        this.hpoDiseases = hpoDiseases;
        this.maxoToHpoTermMap = maxoToHpoTermMap;
        this.hpoToMaxoTermMap = hpoToMaxoTermMap;
        this.hpo = hpo;
        if (this.maxoToHpoTermMap.isEmpty()) {
            System.err.println("maxoToHpoTermMap (hpo Diseases) is empty");
            System.exit(1);
        }
        if (this.hpoToMaxoTermMap.isEmpty()) {
            System.err.println("hpoToMaxoTermMap is empty");
            System.exit(1);
        }
    }

    public DiffDiagRefinerImpl(MdContext context,
                               Map<SimpleTerm, Set<SimpleTerm>> maxoToHpoTermMap,
                               Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap) {
        this.context = context;
        this.hpo = context.resources().hpo();
        this.hpoDiseases = context.resources().hpoDiseases();
        this.maxoToHpoTermMap = maxoToHpoTermMap;
        this.hpoToMaxoTermMap = hpoToMaxoTermMap;
        if (this.maxoToHpoTermMap.isEmpty()) {
            System.err.println("maxoToHpoTermMap (mdContext) is empty");
            System.exit(1);
        }
        if (this.hpoToMaxoTermMap.isEmpty()) {
            System.err.println("hpoToMaxoTermMap is empty");
            System.exit(1);
        }
    }


    @Override
    public List<RankedMaxoResult> run(PhenopacketData sample,
                                      Set<TermId> initialDiagnosesIds,
                                      RankMaxo rankMaxo) throws Exception {
        return rankMaxo.rankMaxoTerms(sample, initialDiagnosesIds, context);
    }

    public List<RankedMaxoResultSingleDisease> runSingleDisease(PhenopacketData sample,
                                                                TermId targetDiseaseId,
                                                                Set<TermId> initialDiagnosesIds,
                                                                RankMaxo rankMaxo) throws Exception {
        return rankMaxo.getDiseaseBestMaxoTerms(sample, targetDiseaseId, initialDiagnosesIds, context);
    }

    public RankMaxo getRankMaxo(List<DifferentialDiagnosis> allInitialDiagnoses,
                                List<DifferentialDiagnosis> initialDiagnoses,
                                DDxEngine engine,
                                Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                                List<HpoFrequency> hpoFrequenciesNDiseases) {


        MaxoHpoTermProbabilities maxoHpoTermProbabilities = new MaxoHpoTermProbabilities(hpoDiseases,
                hpoToMaxoTermMap,
                initialDiagnoses);


        return new RankMaxo(maxoToHpoTermIdMap, maxoHpoTermProbabilities, engine,
                hpo, allInitialDiagnoses, initialDiagnoses, hpoFrequenciesNDiseases);
    }


    //TODO: handle possible multiple differential diagnoses with same termId

    @Override
    public List<DifferentialDiagnosis> getOrderedDiagnoses(Collection<DifferentialDiagnosis> originalDifferentialDiagnoses) {
        if (originalDifferentialDiagnoses.size() < context.params().nDiseases()) {
            //TODO: replace with MaxodiffRuntimeException that extends RuntimeException.
            throw new RuntimeException("Input No. Diseases larger than No. diseases in sample.");
        }
        List<DifferentialDiagnosis> orderedDiagnoses = originalDifferentialDiagnoses.stream()
                .sorted(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed())
                .toList();
        return orderedDiagnoses.subList(0, context.params().nDiseases());
    }

    @Override
    public List<HpoDisease> getDiseases(List<DifferentialDiagnosis> differentialDiagnoses) {
        // Get diseaseIds and then diseases from differential diagnoses list
        //TODO: Set of diseaseIds should be a requirement of the Sample, don't need to define it here necessarily.
        Set<TermId> diseaseIds = differentialDiagnoses.stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toSet());
        List<HpoDisease> diseases = new ArrayList<>();
        diseaseIds.forEach(id -> hpoDiseases.diseaseById(id).ifPresent(diseases::add));

        return diseases;
    }

    @Override
    public List<HpoFrequency> getHpoFrequenciesNDiseases(List<HpoDisease> diseases,
                                                         List<HpoFrequency> allHpoFrequencies) {

        List<HpoFrequency> hpoFrequenciesNDiseases = new ArrayList<>();
        allHpoFrequencies.forEach(freq -> diseases.stream().filter(disease -> freq.diseaseId().equals(disease.id()))
                .map(disease -> freq).forEach(hpoFrequenciesNDiseases::add));

        return hpoFrequenciesNDiseases;
    }

    @Override
    public Map<TermId, Set<TermId>> getMaxoToHpoTermIdMap(List<HpoFrequency> hpoFrequenciesNDiseases) {


        Set<TermId> hpoIds = hpoFrequenciesNDiseases.stream().map(HpoFrequency::hpoId).collect(Collectors.toSet());

        // Get all the MaXo terms that can be used to diagnose the HPO terms, removing ancestors
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = new HashMap<>();
        for (Map.Entry<SimpleTerm, Set<SimpleTerm>> entry : maxoToHpoTermMap.entrySet()) {
            TermId maxoTermId = entry.getKey().tid();
            Set<TermId> maxoHpoTermIds = entry.getValue().stream().map(SimpleTerm::tid).collect(Collectors.toSet());
            maxoHpoTermIds.retainAll(hpoIds);
            for (TermId hpoTermId : maxoHpoTermIds) {
                if (!maxoToHpoTermIdMap.containsKey(maxoTermId)) {
                    maxoToHpoTermIdMap.put(maxoTermId, new HashSet<>(Collections.singleton(hpoTermId)));
                } else {
                    Set<TermId> hpoTermIds = maxoToHpoTermIdMap.get(maxoTermId);
                    hpoTermIds.add(hpoTermId);
                    maxoToHpoTermIdMap.replace(maxoTermId, hpoTermIds);
                }
            }
        }
        //TODO: removing ancestors possibly incorrect for excluded HPO features
        for (Map.Entry<TermId, Set<TermId>> e : maxoToHpoTermIdMap.entrySet()) {
            // Remove HPO ancestor term Ids from list
            TermId mId = e.getKey();
            Set<TermId> hpoIdSet = new HashSet<>(e.getValue());
            for (TermId hpoId : e.getValue()) {
                try {
                    for (TermId ancestor : hpo.graph().getAncestors(hpoId)) {
                        hpoIdSet.remove(ancestor);
                    }
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
            maxoToHpoTermIdMap.replace(mId, hpoIdSet);
        }
        return maxoToHpoTermIdMap;
    }

}
