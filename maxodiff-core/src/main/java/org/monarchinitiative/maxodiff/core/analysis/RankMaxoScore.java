package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record RankMaxoScore(TermId maxoId, Set<TermId> initialOmimTermIds, Set<TermId> maxoOmimTermIds,
                            Set<TermId> discoverableHpoTermIds,
                            Double maxoScore,
                            List<DifferentialDiagnosis> maxoDiagnoses,
                            Map<TermId, Map<TermId, Integer>> hpoTermIdRepCtsMap,
                            Map<TermId, Integer> maxoDiseaseAvgRankChangeMap,
                            int minRankChange,
                            int maxRankChange) {
}
