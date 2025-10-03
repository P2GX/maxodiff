package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;

public class MaxoHpoDiseaseRank {

    private final List<DifferentialDiagnosis> initialDiagnoses;
    private final AscertainablePhenotypes ascertainablePhenotypes;
    private final Set<TermId> allMaxoAscertainedHpoIds;
    private final TermId maxoId;

    List<Integer> ascertainedHpoCountList = new ArrayList<>();
    Map<TermId, List<Double>> hpoToRankMap = new HashMap<>();
    Map<TermId, Double> hpoToProbabiltyMap = new HashMap<>();


    public MaxoHpoDiseaseRank(List<DifferentialDiagnosis> initialDiagnoses,
                              AscertainablePhenotypes ascertainablePhenotypes,
                              Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                              TermId maxoId) {
        this.initialDiagnoses = initialDiagnoses;
        this.ascertainablePhenotypes = ascertainablePhenotypes;
        this.maxoId = maxoId;
        this.allMaxoAscertainedHpoIds = maxoToHpoTermIdMap.get(maxoId);

    }

    public void makeAscertainedHpoCountListAndRankMap(Sample sample, int nDiagnoses) {

        for (DifferentialDiagnosis diagnosis : initialDiagnoses.subList(0, nDiagnoses)) {
            double diseaseRank = 1.0 / (initialDiagnoses.indexOf(diagnosis) + 1);
            TermId diseaseId = diagnosis.diseaseId();
            Set<TermId> diseaseAnnotatedHpoIds = ascertainablePhenotypes.getAscertainablePhenotypeIds(sample, diseaseId);
            // C = count of HPO terms annotated to disease that are ascertained by MAXO
            Set<TermId> intersection = new HashSet<>(diseaseAnnotatedHpoIds);
            intersection.retainAll(allMaxoAscertainedHpoIds);
            int c = intersection.size();
            ascertainedHpoCountList.add(c);

            // HpoToRankMap
            for (TermId hpoId : diseaseAnnotatedHpoIds) {
                hpoToRankMap.putIfAbsent(hpoId, new ArrayList<>());
                hpoToRankMap.get(hpoId).add(diseaseRank);
            }
        }
    }

    public void makeHpoToProbabilityMap(Sample sample) {

        Set<TermId> sampleTerms = new HashSet<>(sample.observedHpoTermIds());
        sampleTerms.addAll(sample.excludedHpoTermIds());
        List<TermId> maxoAscertainedHpoIdsExclSample = new ArrayList<>(allMaxoAscertainedHpoIds);
        maxoAscertainedHpoIdsExclSample.removeAll(sampleTerms);

        Map<TermId, Double> hpoToProbabilityMapOriginal = new HashMap<>();
        for (TermId hpoId : maxoAscertainedHpoIdsExclSample) {
            Double EPSILON = 0.000001; // small probability
            hpoToProbabilityMapOriginal.put(hpoId, EPSILON);
        }
        for (Map.Entry<TermId, List<Double>> hpoRankMapEntry : hpoToRankMap.entrySet()) {
            TermId hpo = hpoRankMapEntry.getKey();
            List<Double> ranks = hpoRankMapEntry.getValue();
            Double rankSum = ranks.stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
            hpoToProbabilityMapOriginal.replace(hpo, rankSum);
        }

        //Normalize probability values and order by decreasing probability
        Double probabilitySum = hpoToProbabilityMapOriginal.values().stream().mapToDouble(Double::doubleValue).sum();
        hpoToProbabilityMapOriginal.forEach( (id, prob) -> hpoToProbabilityMapOriginal.replace(id, prob/probabilitySum));

        hpoToProbabiltyMap = hpoToProbabilityMapOriginal.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b)->b, LinkedHashMap::new));

    }

    public List<Integer> getAscertainedHpoCountList() {
        return ascertainedHpoCountList;
    }

    public Map<TermId, List<Double>> getHpoToRankMap() {
        return hpoToRankMap;
    }

    public Map<TermId, Double> getHpoToProbabiltyMap() {
        return hpoToProbabiltyMap;
    }

    public TermId getMaxoId() {
        return maxoId;
    }
}
