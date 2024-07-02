package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;

public class DummyDiffDiagRefiner implements DiffDiagRefiner {


    private final HpoDiseases hpoDiseases;
    private final Map<TermId, Set<TermId>> fullHpoToMaxoTermMap;
    private final MinimalOntology hpo;

    public DummyDiffDiagRefiner(HpoDiseases hpoDiseases, Map<TermId, Set<TermId>> fullHpoToMaxoTermMap, MinimalOntology hpo) {
        this.hpoDiseases = hpoDiseases;
        this.fullHpoToMaxoTermMap = fullHpoToMaxoTermMap;
        this.hpo = hpo;
    }

    public RefinementResults run(Sample sample,
                                      Collection<DifferentialDiagnosis> originalDifferentialDiagnoses,
                                      RefinementOptions options) {
        // Potential outline:
        // 1) pick MAXO term at random from full MAXO:HPO map
        // 2) get set of HPO terms from MAXO term
        // 3) make HPO combos and calculate score as normal?

        if (originalDifferentialDiagnoses.size() < options.nDiseases()) {
            //TODO: replace with MaxodiffRuntimeException that extends RuntimeException.
            throw new RuntimeException("Input No. Diseases larger than No. diseases in sample.");
        }
        List<DifferentialDiagnosis> orderedDiagnoses = originalDifferentialDiagnoses.stream()
                .sorted(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed())
                .toList();
        List<DifferentialDiagnosis> differentialDiagnoses = orderedDiagnoses.subList(0, options.nDiseases());

        // Get diseaseIds and then diseases from differential diagnoses list
        //TODO: Set of diseaseIds should be a requirement of the Sample, don't need to define it here necessarily.
        Set<TermId> diseaseIds = differentialDiagnoses.stream()
                .map(DifferentialDiagnosis::diseaseId)
                .collect(Collectors.toSet());
        List<HpoDisease> diseases = new ArrayList<>();
        diseaseIds.forEach(id -> hpoDiseases.diseaseById(id).ifPresent(diseases::add));

        // Get Map of HPO Term Id and List of HpoFrequency objects for list of m diseases.
        Map<TermId, List<HpoFrequency>> hpoTermCountsImmutable = AnalysisUtils.getHpoTermCounts(diseases);
        Map<TermId, List<HpoFrequency>> hpoTermCounts = new HashMap<>(hpoTermCountsImmutable);

        // Remove HPO terms present in the sample
        sample.presentHpoTermIds().forEach(hpoTermCounts::remove);
        sample.excludedHpoTermIds().forEach(hpoTermCounts::remove);

        // Get all the MaXo terms that can be used to diagnose the HPO terms, removing ancestors
        //TODO: make MAXO:HPO term map directly from maxo_diagnostic_annotations.tsv file
        Map<TermId, Set<TermId>> hpoToMaxoTermIdMap = AnalysisUtils.makeHpoToMaxoTermIdMap(fullHpoToMaxoTermMap, fullHpoToMaxoTermMap.keySet());
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = AnalysisUtils.makeMaxoToHpoTermIdMap(hpo, hpoToMaxoTermIdMap);

        // Calculate final scores and make list of MaxodiffResult objects.
        List<MaxodiffResult> maxodiffResultsList = new ArrayList<>();
        Random randomizer = new Random();
        int randomInt = randomizer.nextInt(maxoToHpoTermIdMap.size());
        System.out.println("random Int = " + randomInt);
        TermId maxoId = maxoToHpoTermIdMap.keySet().stream().toList().get(randomInt);
//        for (TermId maxoId : maxoToHpoTermIdMap.keySet()) {
        // Get the set of HPO terms that can be ascertained by the MAXO term
        Set<TermId> hpoTermIds = maxoToHpoTermIdMap.get(maxoId);
        // Get HPO term combinations
        List<List<TermId>> hpoCombos = AnalysisUtils.getHpoTermCombos(maxoToHpoTermIdMap, maxoId);
        // Calculate final score and make score record
        MaxoTermScore maxoTermScore = AnalysisUtils.getMaxoTermScoreRecord(hpoTermIds,
                hpoCombos,
                maxoId,
                differentialDiagnoses,
                diseases,
                options);
        // Get HPO frequency records
//            List<Frequencies> frequencies = getFrequencyRecords(maxoTermScore, hpoTermCounts);
        // Make MaxodiffResult for the MAXO term
        MaxodiffResult maxodiffResult = new MaxodiffResultImpl(maxoTermScore, List.of());
        maxodiffResultsList.add(maxodiffResult);
//        }

        // Return RefinementResults object, which contains the list of MaxodiffResult objects.
        return new RefinementResultsImpl(maxodiffResultsList);
    }
}
