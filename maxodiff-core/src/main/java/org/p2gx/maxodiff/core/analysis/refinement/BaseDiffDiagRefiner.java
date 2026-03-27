package org.p2gx.maxodiff.core.analysis.refinement;

import org.p2gx.maxodiff.core.analysis.HpoFrequency;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;
import org.p2gx.maxodiff.core.analysis.SimpleTerm;
import org.p2gx.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.p2gx.maxodiff.core.model.*;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;

public class BaseDiffDiagRefiner implements DiffDiagRefiner {

    private final HpoDiseases hpoDiseases;
    private final Map<String, Set<String>> fullHpoToMaxoTermIdMap;
    private final Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap;
    private final Ontology hpo;

    public BaseDiffDiagRefiner(HpoDiseases hpoDiseases,
                               Map<String, Set<String>> fullHpoToMaxoTermIdMap,
                               Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap,
                               Ontology hpo) {
        this.hpoDiseases = hpoDiseases;
        this.fullHpoToMaxoTermIdMap = fullHpoToMaxoTermIdMap;
        this.hpoToMaxoTermMap = hpoToMaxoTermMap;
        this.hpo = hpo;
    }


    @Override
    public List<RankedMaxoResult> runNew(PpktSample sample,
                                         Set<TermId> initialDiagnosesIds,
                                         RefinementOptions options,
                                         RankMaxo rankMaxo,
                                         BiometadataService biometadataService) throws Exception {


        return rankMaxo.rankMaxoTermsNew(sample, options.nRepetitions(),
                initialDiagnosesIds, biometadataService);
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

    @Override
    public List<DifferentialDiagnosis> getOrderedDiagnoses(Collection<DifferentialDiagnosis> originalDifferentialDiagnoses,
                                                           RefinementOptions options) {
        if (originalDifferentialDiagnoses.size() < options.nDiseases()) {
            //TODO: replace with MaxodiffRuntimeException that extends RuntimeException.
            throw new RuntimeException("Input No. Diseases larger than No. diseases in sample.");
        }
        List<DifferentialDiagnosis> orderedDiagnoses = originalDifferentialDiagnoses.stream()
                .sorted(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed())
                .toList();

        return orderedDiagnoses.subList(0, options.nDiseases());
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
