package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.MinimalOntology;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;

public class MaxoDiffRefiner implements DiffDiagRefiner {

    private final HpoDiseases hpoDiseases;
    private final Map<TermId, Set<TermId>> fullHpoToMaxoTermMap;
    private final MinimalOntology hpo;

    public MaxoDiffRefiner(HpoDiseases hpoDiseases, Map<TermId, Set<TermId>> fullHpoToMaxoTermMap,
                           MinimalOntology hpo) {
        this.hpoDiseases = hpoDiseases;
        this.fullHpoToMaxoTermMap = fullHpoToMaxoTermMap;
        this.hpo = hpo;
    }


    @Override
    public RefinementResults run(Sample sample,
                                 Collection<DifferentialDiagnosis> originalDifferentialDiagnoses,
                                 RefinementOptions options) {
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
        Set<TermId> hpoIds = hpoTermCounts.keySet();

        // Get all the MaXo terms that can be used to diagnose the HPO terms, removing ancestors
        //TODO: make MAXO:HPO term map directly from maxo_diagnostic_annotations.tsv file
        Map<TermId, Set<TermId>> hpoToMaxoTermIdMap = AnalysisUtils.makeHpoToMaxoTermIdMap(fullHpoToMaxoTermMap, hpoIds);
        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = AnalysisUtils.makeMaxoToHpoTermIdMap(hpo, hpoToMaxoTermIdMap);

        // Calculate final scores and make list of MaxodiffResult objects.
        List<MaxodiffResult> maxodiffResultsList = new ArrayList<>();
        for (TermId maxoId : maxoToHpoTermIdMap.keySet()) {
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
            List<Frequencies> frequencies = getFrequencyRecords(maxoTermScore, hpoTermCounts);
            // Make MaxodiffResult for the MAXO term
            MaxodiffResult maxodiffResult = new MaxodiffResultImpl(maxoTermScore, frequencies);
            maxodiffResultsList.add(maxodiffResult);
        }

        // Return RefinementResults object, which contains the list of MaxodiffResult objects.
        return new RefinementResultsImpl(maxodiffResultsList);
    }

    //TODO: handle possible multiple differential diagnoses with same termId

    /**
     *
     * @param maxoTermScoreRecord MaxoTermScore record.
     * @param hpoTermCounts Map of HPO Term Id and List of HpoFrequency objects.
     * @return List of Frequencies records
     */
    private static List<Frequencies> getFrequencyRecords(MaxoTermScore maxoTermScoreRecord,
                                                 Map<TermId, List<HpoFrequency>> hpoTermCounts) {

        List<Frequencies> frequencyRecords = new ArrayList<>();
        Set<TermId> omimIds = maxoTermScoreRecord.omimTermIds();
        for (TermId hpoId : maxoTermScoreRecord.hpoTermIds()) {
            Map<TermId, Float> maxoFrequencies = new LinkedHashMap<>();
            omimIds.forEach(e -> maxoFrequencies.put(e, null));
            List<HpoFrequency> frequencies = hpoTermCounts.get(hpoId);
            for (HpoFrequency hpoFrequency : frequencies) {
                for (TermId omimId : omimIds) {
                    if (hpoFrequency.omimId().equals(omimId.toString())) {
                        Float frequency = hpoFrequency.frequency();
                        maxoFrequencies.replace(omimId, frequency);
                    }
                }
            }
            frequencyRecords.add(new Frequencies(hpoId, maxoFrequencies.values().stream().toList()));
        }
        return frequencyRecords;
    }

}
