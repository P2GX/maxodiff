package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HTMLFrequencyMap {

    public static List<HpoFrequency> getHpoFrequencies(Map<TermId, List<HpoFrequency>> hpoTermCounts) {
        List<HpoFrequency> freqRecords = new ArrayList<>();
        for (Map.Entry<TermId, List<HpoFrequency>> entry : hpoTermCounts.entrySet()) {
            var freqRecordList = entry.getValue();
            freqRecords.addAll(freqRecordList);
        }

        return freqRecords;
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
                        if (freqRecordOmimId.equals(omimId.toString()) && freqRecordHpoId.equals(hpoId.toString()) && frequency != null) {
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
