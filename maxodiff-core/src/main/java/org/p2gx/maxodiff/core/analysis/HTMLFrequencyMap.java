package org.p2gx.maxodiff.core.analysis;

import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;

import java.util.*;

public class HTMLFrequencyMap {
    private final HpoDiseases diseases;
    private final Map<TermPair, Double> icMicaData;

    public HTMLFrequencyMap(
            HpoDiseases diseases,
            Map<TermPair, Double> icMicaData
    ) {
        this.diseases = diseases;
        this.icMicaData = icMicaData;
    }


    /**
     * Retrieve a flattened list of {@link HpoFrequency} records from the provided map.
     *
     * <p>Each {@link HpoFrequency} has an OMIM id, an HPO id, and a frequency..</p>
     *
     * @param hpoTermCounts a map where each key is an {@link TermId} and the value is a list of
     *                      {@link HpoFrequency} objects associated with that term
     * @return a combined list of all {@link HpoFrequency} objects across all HPO terms
     */
    public static List<HpoFrequency> getHpoFrequencies(Map<TermId, List<HpoFrequency>> hpoTermCounts) {
        return hpoTermCounts.values().stream()
                .flatMap(List::stream)
                .toList();
    }




    /**
     * @param hpoId target HPO term
     * @param diseaseId target OMIM disease
     * @return maximum MICA for the HPO term and any of the disease observed HPO terms
     */
    public float micaForDisease(TermId hpoId, TermId diseaseId) {
        Optional<HpoDisease> opt = this.diseases.diseaseById(diseaseId);
        if (opt.isEmpty()) {
            return 0f;
        }
        HpoDisease disease = opt.get();
        List<TermId> diseaseHpoTermIds = disease.presentAnnotationsStream()
                .map(HpoDiseaseAnnotation::id)
                .toList();
        float mica = 0f;
        for (TermId tid : diseaseHpoTermIds) {
            TermPair tp = TermPair.symmetric(tid, hpoId);
            double m = icMicaData.getOrDefault(tp, 0d);
            if (m > mica) mica = (float) m;
        }
        return mica;
    }

    public static Map<String, Map<Float, List<String>>> makeFrequencyDiseaseMap(Map<TermId, String> hpoIdToLabelMap,
                                                                                Map<TermId, String> omimIdToLabelMap,
                                                                                Map<TermId, Map<TermId, Integer>> hpoTermIdRepCtsMap,
                                                                                List<HpoFrequency> hpoFrequencies) {

        Map<String, Map<Float, List<String>>> frequencyMap = new HashMap<>();
        for (Map.Entry<TermId, String> hpoMapEntry : hpoIdToLabelMap.entrySet()) {
            var hpoId = hpoMapEntry.getKey();
            var hpoLabel = hpoMapEntry.getValue();
            Map<Float, List<String>> frequencyOmimMap = new HashMap<>();
            for (Map.Entry<TermId, String> omimMapEntry : omimIdToLabelMap.entrySet()) {
                var omimId = omimMapEntry.getKey();
                var omimLabel = omimMapEntry.getValue();
                var hpoRepCt = hpoTermIdRepCtsMap.get(omimId).get(hpoId);
                if (hpoRepCt != null) {
                    for (HpoFrequency freqRecord : hpoFrequencies) {
                        var freqRecordOmimId = freqRecord.omimId();
                        var freqRecordHpoId = freqRecord.hpoId();
                        var frequency = freqRecord.frequency();
                        if (freqRecordOmimId.equals(omimId.toString()) && freqRecordHpoId.equals(hpoId.toString()) && frequency > 0) {
                            List<String> frequencyMapDiseaseList = new ArrayList<>();
                            if (!frequencyOmimMap.containsKey(frequency)) {
                                frequencyMapDiseaseList.add(omimLabel);
                            } else {
                                frequencyMapDiseaseList = frequencyOmimMap.get(frequency);
                                frequencyMapDiseaseList.add(omimLabel);
                            }
                            frequencyOmimMap.put(frequency, frequencyMapDiseaseList);
                        }
                    }
                }
            }
            if (!frequencyOmimMap.isEmpty()) {
                frequencyMap.put(hpoLabel, frequencyOmimMap);
            }
        }

        return frequencyMap;
    }
}
