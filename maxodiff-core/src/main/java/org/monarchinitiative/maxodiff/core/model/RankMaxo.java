package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.MaxoDDResults;
import org.monarchinitiative.maxodiff.core.analysis.RankMaxoScore;
import org.monarchinitiative.maxodiff.core.analysis.ValidationModel;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;

public class RankMaxo {

    private final Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap;
    private final Map<TermId, Set<TermId>> maxoToHpoTermIdMap;
    private final MaxoHpoTermProbabilities maxoHpoTermProbabilities;
    private final DifferentialDiagnosisEngine engine;
    double p;
    private final MinimalOntology minimalOntology;
    private final Ontology ontology;

    public RankMaxo(Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap,
                    Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                    MaxoHpoTermProbabilities maxoHpoTermProbabilities,
                    DifferentialDiagnosisEngine engine,
                    MinimalOntology minHpo,
                    Ontology hpo) {
        this.hpoToMaxoTermMap = hpoToMaxoTermMap;
        this.maxoToHpoTermIdMap = maxoToHpoTermIdMap;
        this.maxoHpoTermProbabilities = maxoHpoTermProbabilities;
        this.engine = engine;
        this.minimalOntology = minHpo;
        this.ontology = hpo;
    }

    /**
     *
     * @param ppkt Input phenopacket with present and excluded HPO terms.
     * @param nRepetitions number of times to calculate scores for each MAxO term.
     * @param diseaseIds Set of OMIM disease Ids to use for analysis.
     * @return Map of MAxO scores sorted in descending order by score
     */
    public Map<TermId, RankMaxoScore> rankMaxoTerms(Sample ppkt, int nRepetitions, Set<TermId> diseaseIds) {
        Map<TermId, RankMaxoScore> maxoScores = new HashMap<>();
        CandidateDiseaseScores candidateDiseaseScores = new CandidateDiseaseScores(maxoHpoTermProbabilities, minimalOntology, ontology);
        p = 0;
        Set<TermId> sampleHpoIds = new HashSet<>();
        sampleHpoIds.addAll(ppkt.presentHpoTermIds());
        sampleHpoIds.addAll(ppkt.excludedHpoTermIds());

        for (TermId maxoId : maxoToHpoTermIdMap.keySet()) {
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


            RankMaxoScore rankMaxoScore = new RankMaxoScore(maxoId, initialDiagnosesDiseaseIds, maxoDiagnosesDiseaseIds,
                    maxoDiscoverableObservedHpoIds, meanScore, maxoDDResultsList.getLast().maxoDifferentialDiagnoses(),
                    maxoDiscoverableHpoIdCtsSorted, maxoDiseaseAvgRankChangeMapSorted,
                    Collections.min(maxoDiseaseAvgRankChangeMapSorted.values()), Collections.max(maxoDiseaseAvgRankChangeMapSorted.values()));
            maxoScores.put(maxoId, rankMaxoScore);
            p++;
            updateProgress();
        }

        return maxoScores.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<TermId, RankMaxoScore> e) -> e.getValue().maxoScore())
                .reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a,b)->b, LinkedHashMap::new));
    }

    private static double calculateRelDiseaseDiffEntropySum(List<Double> differentialDiagnosisScores) {
        double sum = 0.0;
        final double EPSILON = 1e-10;
        for(Double score : differentialDiagnosisScores) {
            //TODO: double-check log base. Default is base e.
            sum += Math.abs(score) < EPSILON ? 0 : Math.log(score)*score;
        }
        return -sum;
    }

    public double updateProgress() {
        int nMaxoTermIds = maxoToHpoTermIdMap.keySet().size();
        return (p / nMaxoTermIds) * 100.;

    }

}
