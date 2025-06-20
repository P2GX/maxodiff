package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.MaxoDDResults;
import org.monarchinitiative.maxodiff.core.analysis.MaxoHpoTermIdMaps;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.service.DfsHpoTermArranger;
import org.monarchinitiative.maxodiff.core.service.HpoTermArranger;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.algo.OntologyAlgorithm;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.Ontology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CandidateDiseaseScores {

    private final MaxoHpoTermProbabilities maxoHpoTermProbabilities; //contains top K initial diagnoses only
    private final MinimalOntology minimalOntology;
    private final Ontology ontology;

    public CandidateDiseaseScores(MaxoHpoTermProbabilities maxoHpoTermProbabilities, MinimalOntology minHpo, Ontology hpo) {
        this.maxoHpoTermProbabilities = maxoHpoTermProbabilities;
        this.minimalOntology = minHpo;
        this.ontology = hpo;
    }

    /**
     *
     * @param ppkt Input phenopacket with present and excluded HPO terms.
     * @param maxoId TermId of the MAxO term of interest.
     * @param engine Engine to use for the differential diagnosis, e.g. LIRICAL.
     * @return List of the top K differential diagnoses for the given MAxO term.
     */
    public MaxoDDResults getScoresForMaxoTerm(Sample ppkt, TermId maxoId,
                                              DifferentialDiagnosisEngine engine,
                                              Set<TermId> diseaseIds,
                                              Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap) {
        Set<TermId> observed = new HashSet<>(Set.of());
        Set<TermId> excluded = new HashSet<>(Set.of());

        //TODO: some of these maps/probabilities can go into the constructor
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = MaxoHpoTermIdMaps.getMaxoToHpoTermIdMap(hpoToMaxoTermMap);
        HpoDiseases hpoDiseases = maxoHpoTermProbabilities.getHpoDiseases();
        Set<TermId> maxoBenefitHpoIds = maxoHpoTermProbabilities.getDiscoverableByMaxoHpoTerms(ppkt, maxoId, maxoToHpoTermIdMap);
        List<TermId> diseaseIdList = new LinkedList<>(diseaseIds);
        Map<TermId, Double> diseaseRankProbabilityMap = new LinkedHashMap<>();
        diseaseIds.stream().forEach(id -> diseaseRankProbabilityMap.put(id, 1./(diseaseIdList.indexOf(id)+1)));
        Double diseaseRankProbabilityValueSum = diseaseRankProbabilityMap.values().stream().mapToDouble(d->d).sum();
        diseaseRankProbabilityMap.forEach((diseaseIdKey, probabilityValue) ->
                diseaseRankProbabilityMap.replace(diseaseIdKey, probabilityValue / diseaseRankProbabilityValueSum));
        AscertainablePhenotypes ascertainablePhenotypes = new AscertainablePhenotypes(hpoDiseases);
        TermId selectedDiseaseId = getDiseaseId(diseaseRankProbabilityMap);
        Set<TermId> ascertainablePhenotypeIds = ascertainablePhenotypes.getAscertainablePhenotypeIds(ppkt, selectedDiseaseId);
        Set<TermId> maxoAddedObservedHpoIds = new HashSet<>();
        Set<TermId> maxoAddedObservedDescendantHpoIds = new HashSet<>();
        Set<TermId> maxoAddedExcludedHpoIds = new HashSet<>();
        for (TermId hpoId : ascertainablePhenotypeIds) {
            if (maxoBenefitHpoIds.contains(hpoId)) {
                if (!excluded.contains(hpoId)) {
                    observed.add(hpoId);
                    maxoAddedObservedHpoIds.add(hpoId);
                } else if (!observed.contains(hpoId)) {
                    excluded.add(hpoId);
                    maxoAddedExcludedHpoIds.add(hpoId);
                }
            }
            Set<TermId> ascertainablePhenotypeDescendants = ontology.graph().getDescendantSet(hpoId);
            for (TermId descHpoId : ascertainablePhenotypeDescendants) {
                if (maxoBenefitHpoIds.contains(descHpoId)) {
                    if (!excluded.contains(descHpoId)) {
                        observed.add(descHpoId);
                        maxoAddedObservedHpoIds.add(descHpoId);
                        maxoAddedObservedDescendantHpoIds.add(descHpoId);
                    } else {
                        for (TermId maxoHpoId : maxoBenefitHpoIds) {
                            if (OntologyAlgorithm.termsAreRelated(ontology, descHpoId, maxoHpoId)) {
                                if (!excluded.contains(descHpoId)) {
                                    observed.add(descHpoId);
                                    maxoAddedObservedHpoIds.add(descHpoId);
                                    maxoAddedObservedDescendantHpoIds.add(descHpoId);
                                }
                            } else {
                                if (!observed.contains(descHpoId)) {
                                    excluded.add(descHpoId);
                                    maxoAddedExcludedHpoIds.add(descHpoId);
                                }
                            }
                        }
                    }
                }
            }
        }

        Sample newSample = getNewSample(ppkt, observed, excluded);
        List<DifferentialDiagnosis> newMaxoDiagnoses = engine.run(newSample, diseaseIds);

        HpoTermArranger hpoTermArranger = new DfsHpoTermArranger(ontology);
        Set<TermId> maxoAddedObservedHpoIdsOrdered = new HashSet<>(hpoTermArranger.arrangeTerms(maxoAddedObservedHpoIds.stream().toList()));
        Set<TermId> maxoAddedExcludedHpoIdsOrdered = new HashSet<>(hpoTermArranger.arrangeTerms(maxoAddedExcludedHpoIds.stream().toList()));

        return new MaxoDDResults(maxoAddedObservedHpoIdsOrdered, maxoAddedExcludedHpoIdsOrdered,
                maxoAddedObservedDescendantHpoIds, newMaxoDiagnoses);
    }

    private boolean getTestResult(double maxoTermBenefitProbability) {
        // Generate random number between 0 and 1
        double randomNumber = Math.random();

        return randomNumber > maxoTermBenefitProbability;
    }

    private TermId getDiseaseId(Map<TermId, Double> diseaseRankProbabilityMap) {
        // Generate random number between 0 and 1
        double randomNumber = Math.random();

        double closestMapValue = diseaseRankProbabilityMap.values().stream()
                .min(Comparator.comparingDouble(i -> Math.abs(i - randomNumber))).get();

        TermId selectedDiseaseId = null;
        for (Map.Entry<TermId, Double> entry : diseaseRankProbabilityMap.entrySet()) {
            TermId diseaseId = entry.getKey();
            Double probabilityValue = entry.getValue();
            if (probabilityValue.equals(closestMapValue)) {
                selectedDiseaseId = diseaseId;
            }
        }
        return selectedDiseaseId;
    }

    private Sample getNewSample(Sample ppkt, Set<TermId> observed, Set<TermId> excluded) {
        Set<TermId> ppktObserved = new HashSet<>(ppkt.presentHpoTermIds());
        Set<TermId> ppktExcluded = new HashSet<>(ppkt.excludedHpoTermIds());
        Set<TermId> newObserved = Stream.concat(ppktObserved.stream(), observed.stream()).collect(Collectors.toSet());
        Set<TermId> newExcluded = Stream.concat(ppktExcluded.stream(), excluded.stream()).collect(Collectors.toSet());

        return Sample.of(ppkt.id(), newObserved, newExcluded);
    }
}
