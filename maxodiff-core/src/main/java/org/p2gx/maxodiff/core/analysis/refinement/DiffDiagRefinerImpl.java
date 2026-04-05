package org.p2gx.maxodiff.core.analysis.refinement;


import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import org.p2gx.maxodiff.core.diffdg.DifferentialDiagnosisEngine;

import org.p2gx.maxodiff.core.io.MdContext;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import org.p2gx.maxodiff.core.analysis.HpoFrequency;
import org.p2gx.maxodiff.core.model.*;


import java.util.*;
import java.util.stream.Collectors;

public class DiffDiagRefinerImpl implements DiffDiagRefiner {

    private final HpoDiseases hpoDiseases;
    private final Map<String, Set<String>> fullHpoToMaxoTermIdMap;
    private final Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap;
    private final MinimalOntology hpo;

    private MdContext context;

    public DiffDiagRefinerImpl(HpoDiseases hpoDiseases,
                               Map<String, Set<String>> fullHpoToMaxoTermIdMap,
                               Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap,
                               MinimalOntology hpo) {
        this.hpoDiseases = hpoDiseases;
        this.fullHpoToMaxoTermIdMap = fullHpoToMaxoTermIdMap;
        this.hpoToMaxoTermMap = hpoToMaxoTermMap;
        this.hpo = hpo;
        if (this.fullHpoToMaxoTermIdMap.isEmpty()) {
            System.err.println("DiffDiagRefinerImpl is empty");
            System.exit(1);
        }
        if (this.hpoToMaxoTermMap.isEmpty()) {
            System.err.println("DiffDiagRefinerImpl is empty");
            System.exit(1);
        }
    }

    public DiffDiagRefinerImpl(MdContext context,
                               Map<String, Set<String>> fullHpoToMaxoTermIdMap,
                               Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap) {
        this.context = context;
        this.hpo = context.resources().hpo();
        this.hpoDiseases = context.resources().hpoDiseases();
        this.fullHpoToMaxoTermIdMap = fullHpoToMaxoTermIdMap;
        this.hpoToMaxoTermMap = hpoToMaxoTermMap;
        if (this.fullHpoToMaxoTermIdMap.isEmpty()) {
            System.err.println("DiffDiagRefinerImpl is empty");
            System.exit(1);
        }
        if (this.hpoToMaxoTermMap.isEmpty()) {
            System.err.println("DiffDiagRefinerImpl is empty");
            System.exit(1);
        }
    }


    @Override
    public List<RankedMaxoResult> run(PpktSample sample,
                                         Set<TermId> initialDiagnosesIds,
                                         RankMaxo rankMaxo,
                                      List<TermId> ppktMaxoIds) throws Exception {


        return rankMaxo.rankMaxoTerms(sample, context.params().nRepetitions(),
                initialDiagnosesIds, context.biometadataService(), ppktMaxoIds);
    }

    public RankMaxo getRankMaxo(List<DifferentialDiagnosis> allInitialDiagnoses,
                                List<DifferentialDiagnosis> initialDiagnoses,
                                 DifferentialDiagnosisEngine engine,
                                Map<String, Set<String>> maxoToHpoTermIdMap) {

        DiseaseModelProbability diseaseModelProbability = DiseaseModelProbability.ranked(initialDiagnoses);

        MaxoHpoTermProbabilities maxoHpoTermProbabilities = new MaxoHpoTermProbabilities(hpoDiseases,
                hpoToMaxoTermMap,
                initialDiagnoses,
                diseaseModelProbability);


        return new RankMaxo(hpoToMaxoTermMap, maxoToHpoTermIdMap, maxoHpoTermProbabilities, engine,
                hpo, allInitialDiagnoses, initialDiagnoses);
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
    public Map<String, List<HpoFrequency>> getHpoTermCounts(List<HpoDisease> diseases) {

        // Get Map of HPO Term Id and List of HpoFrequency objects for list of m diseases.
        Map<String, List<HpoFrequency>> hpoTermCountsImmutable = AnalysisUtils.getHpoTermCounts(diseases);

        return new HashMap<>(hpoTermCountsImmutable);
    }

    @Override
    public Map<String, Set<String>> getMaxoToHpoTermIdMap(Map<String, List<HpoFrequency>> hpoTermCounts) {


        Set<String> hpoIds = hpoTermCounts.keySet();

        // Get all the MaXo terms that can be used to diagnose the HPO terms, removing ancestors
        //TODO: make MAXO:HPO term map directly from maxo_diagnostic_annotations.tsv file
        Map<String, Set<String>> hpoToMaxoTermIdMap = AnalysisUtils.makeHpoToMaxoTermIdMap(fullHpoToMaxoTermIdMap, hpoIds);
        Map<String, Set<String>> maxoToHpoTermIdMap = AnalysisUtils.makeMaxoToHpoTermIdMap(hpo, hpoToMaxoTermIdMap);

        return maxoToHpoTermIdMap;
    }

}
