package org.p2gx.maxodiff.core.analysis.refinement;

import org.p2gx.maxodiff.core.analysis.HpoFrequency;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;
import org.p2gx.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;
import org.p2gx.maxodiff.core.model.PpktSample;
import org.p2gx.maxodiff.core.model.RankMaxo;
import org.p2gx.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Differential diagnosis results come from some source.
 * We don't expect to get differential diagnosis results for all possible diseases, e.g. the entire OMIM corpus.
 * We don't expect the collection of differential diagnoses to be in any particular order.
 * The MAxO terms are returned in unspecified order.
 */
public interface DiffDiagRefiner {

    List<RankedMaxoResult> run(PpktSample sample,
                               Set<TermId> initialDiagnosesIds,
                               RefinementOptions options,
                               RankMaxo rankMaxo,
                               BiometadataService biometadataService,
                               List<TermId> ppktMaxoIds
    ) throws Exception;

    List<DifferentialDiagnosis> getOrderedDiagnoses(Collection<DifferentialDiagnosis> originalDifferentialDiagnoses,
                                                    RefinementOptions options);

    List<HpoDisease> getDiseases(List<DifferentialDiagnosis> differentialDiagnoses);

    Map<String, List<HpoFrequency>> getHpoTermCounts(List<HpoDisease> hpoDiseases);

    Map<String, Set<String>> getMaxoToHpoTermIdMap(Map<String, List<HpoFrequency>> hpoTermCounts);

    RankMaxo getRankMaxo(List<DifferentialDiagnosis> allInitialDiagnoses,
                         List<DifferentialDiagnosis> initialDiagnoses,
                         DifferentialDiagnosisEngine engine,
                         Map<String, Set<String>> maxoToHpoTermIdMap);
}
