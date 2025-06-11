package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.CandidateDiseaseScores;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.MaxoHpoTermProbabilities;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class EvaluateMaxoTerm implements Callable<RankMaxoScore> {

    private final Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap;
    private final Map<TermId, Set<TermId>> maxoToHpoTermIdMap;
    private final MaxoHpoTermProbabilities maxoHpoTermProbabilities;
    private final DifferentialDiagnosisEngine engine;
    private final MinimalOntology minimalOntology;
    private final Ontology ontology;
    private final Set<TermId> sampleHpoIds;
    private final Sample ppkt;
    private final int nRepetitions;
    private final Set<TermId> diseaseIds;
    private final TermId maxoId;
    CandidateDiseaseScores candidateDiseaseScores;

    public EvaluateMaxoTerm(Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap, Map<TermId,
            Set<TermId>> maxoToHpoTermIdMap, MaxoHpoTermProbabilities maxoHpoTermProbabilities,
                            DifferentialDiagnosisEngine engine, MinimalOntology minimalOntology,
                            Ontology ontology, Set<TermId> sampleHpoIds, Sample ppkt, int nRepetitions,
                            Set<TermId> diseaseIds, TermId maxoId) {
        this.hpoToMaxoTermMap = hpoToMaxoTermMap;
        this.maxoToHpoTermIdMap = maxoToHpoTermIdMap;
        this.maxoHpoTermProbabilities = maxoHpoTermProbabilities;
        this.engine = engine;
        this.minimalOntology = minimalOntology;
        this.ontology = ontology;

        this.sampleHpoIds = sampleHpoIds;
        this.ppkt = ppkt;
        this.nRepetitions = nRepetitions;
        this.diseaseIds = diseaseIds;
        this.maxoId = maxoId;
        this.candidateDiseaseScores = new CandidateDiseaseScores(maxoHpoTermProbabilities, minimalOntology, ontology);
    }


    @Override
    public RankMaxoScore call() throws Exception {
        Set<TermId> maxoBenefitHpoIds = maxoHpoTermProbabilities.getDiscoverableByMaxoHpoTerms(ppkt, maxoId, maxoToHpoTermIdMap);
        for (TermId maxoHpoId : maxoBenefitHpoIds) {
            if (sampleHpoIds.contains(maxoHpoId)) {
                continue;
            }
            Set<TermId> maxoHpoDescendants = ontology.graph().getDescendantSet(maxoHpoId);
            for (TermId maxoHpoDescId : maxoHpoDescendants) {
                if (sampleHpoIds.contains(maxoHpoDescId)) {
                    continue;
                }
            }
        }
        List<Double> scores = new ArrayList<>();
        List<DifferentialDiagnosis> initialDiagnoses = maxoHpoTermProbabilities.getInitialDiagnoses();
        List<MaxoDDResults> maxoDDResultsList = new ArrayList<>();
        Map<TermId, Map<TermId, Integer>> maxoDiscoverableHpoIdCts = new HashMap<>();
        for (int i = 0; i < nRepetitions; i++) {
            MaxoDDResults maxoDDResults = candidateDiseaseScores.getScoresForMaxoTerm(ppkt, maxoId, engine, diseaseIds, hpoToMaxoTermMap);
            maxoDDResultsList.add(maxoDDResults);
            Set<TermId> discoverableHpoIds = maxoDDResults.maxoDiscoverableHpoIds();
            for (TermId diseaseId : diseaseIds) {
                List<TermId> diseaseAssociatedHpoIds = List.of();
                Optional<HpoDisease> opt = maxoHpoTermProbabilities.getHpoDiseases().diseaseById(diseaseId);
                if (opt.isPresent()) {
                    HpoDisease disease = opt.get();
                    diseaseAssociatedHpoIds = disease.annotationTermIdList();
                }
                if (!maxoDiscoverableHpoIdCts.containsKey(diseaseId)) {
                    maxoDiscoverableHpoIdCts.put(diseaseId, new HashMap<>());
                }
                Map<TermId, Integer> hpoIdCtsMap = maxoDiscoverableHpoIdCts.get(diseaseId);
                for (TermId discoverableHpoId : discoverableHpoIds) {
                    if (!hpoIdCtsMap.containsKey(discoverableHpoId)) {
                        if (diseaseAssociatedHpoIds.contains(discoverableHpoId)) {
                            hpoIdCtsMap.put(discoverableHpoId, 1);
                        } else {
                            hpoIdCtsMap.put(discoverableHpoId, null);
                        }
                    } else {
                        Integer ct = hpoIdCtsMap.get(discoverableHpoId);
                        if (ct != null) {
                            hpoIdCtsMap.replace(discoverableHpoId, ct + 1);
                        }
                    }
                    maxoDiscoverableHpoIdCts.replace(diseaseId, hpoIdCtsMap);
                }
            }
            double finalScore = ValidationModel.weightedRankDiff(initialDiagnoses, maxoDDResults.maxoDifferentialDiagnoses()).validationScore();
            scores.add(finalScore);
        }
        OptionalDouble meanScoreOptional = scores.stream().mapToDouble(s -> s).average();
        double meanScore = 0.0;
        if (meanScoreOptional.isPresent()) {
            meanScore = meanScoreOptional.getAsDouble();
        }

        Set<TermId> initialDiagnosesDiseaseIds = initialDiagnoses.stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toSet());
        Set<TermId> maxoDiagnosesDiseaseIds = maxoDDResultsList.getLast().maxoDifferentialDiagnoses().stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toSet());

        Set<TermId> maxoDiscoverableObservedHpoIds = maxoDDResultsList.stream()
                .map(MaxoDDResults::maxoDiscoverableHpoIds)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        Map<TermId, Integer> maxoDiseaseAvgRankChangeMap = new HashMap<>();
        for (TermId omimId : maxoDiagnosesDiseaseIds) {
            int initialRank = 0;
            List<DifferentialDiagnosis> initialDiffDiagnoses = initialDiagnoses.stream()
                    .filter(dd -> dd.diseaseId().equals(omimId)).toList();
            if (!initialDiffDiagnoses.isEmpty()) {
                DifferentialDiagnosis initialDiagnosis = initialDiffDiagnoses.getFirst();
                initialRank = initialDiagnoses.indexOf(initialDiagnosis) + 1;
            }

            List<Integer> rankDiffs = new ArrayList<>();
            int meanRankDiff = 0;
            for (MaxoDDResults maxoDDResults : maxoDDResultsList) {
                List<DifferentialDiagnosis> maxoDiagnoses = maxoDDResults.maxoDifferentialDiagnoses().stream()
                        .filter(dd -> dd.diseaseId().equals(omimId)).toList();
                if (!maxoDiagnoses.isEmpty()) {
                    DifferentialDiagnosis maxoDiagnosis = maxoDiagnoses.getFirst();
                    int maxoRank = maxoDDResults.maxoDifferentialDiagnoses().indexOf(maxoDiagnosis) + 1;
                    int maxoRankDiff = maxoRank - initialRank;
                    rankDiffs.add(maxoRankDiff);
                }
            }

            OptionalDouble meanRankDiffOptional = rankDiffs.stream().mapToDouble(s -> s).average();
            if (meanRankDiffOptional.isPresent()) {
                double meanRankDiffDouble = meanRankDiffOptional.getAsDouble();
                meanRankDiff = (int) Math.round(meanRankDiffDouble);
            }
            maxoDiseaseAvgRankChangeMap.put(omimId, meanRankDiff);
        }
        //sort maps by disease average rank change
        Map<TermId, Integer> maxoDiseaseAvgRankChangeMapSorted = maxoDiseaseAvgRankChangeMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a,b)->b, LinkedHashMap::new));

        Map<TermId, Map<TermId, Integer>> maxoDiscoverableHpoIdCtsSorted = maxoDiseaseAvgRankChangeMapSorted.keySet().stream()
                .filter(maxoDiscoverableHpoIdCts::containsKey)
                .collect(Collectors.toMap(
                        key -> key,
                        maxoDiscoverableHpoIdCts::get,
                        (oldValue, newValue) -> newValue,
                        LinkedHashMap::new
                ));


        return new RankMaxoScore(maxoId, initialDiagnosesDiseaseIds, maxoDiagnosesDiseaseIds,
                maxoDiscoverableObservedHpoIds, meanScore, maxoDDResultsList.getLast().maxoDifferentialDiagnoses(),
                maxoDiscoverableHpoIdCtsSorted, maxoDiseaseAvgRankChangeMapSorted,
                Collections.min(maxoDiseaseAvgRankChangeMapSorted.values()), Collections.max(maxoDiseaseAvgRankChangeMapSorted.values()));
    }
}
