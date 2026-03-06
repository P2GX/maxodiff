package org.monarchinitiative.maxodiff.core.analysis.refinement;

import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.analysis.RankedMaxoResult;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.PpktSample;
import org.monarchinitiative.maxodiff.core.model.RankMaxo;
import org.monarchinitiative.maxodiff.core.service.BiometadataService;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
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

    List<RankedMaxoResult> runNew(PpktSample sample,
                                  Set<TermId> initialDiagnosesIds,
                                  RefinementOptions options,
                                  RankMaxo rankMaxo,
                                  BiometadataService biometadataService
    ) throws Exception;

    List<DifferentialDiagnosis> getOrderedDiagnoses(Collection<DifferentialDiagnosis> originalDifferentialDiagnoses,
                                                    RefinementOptions options);

    List<HpoDisease> getDiseases(List<DifferentialDiagnosis> differentialDiagnoses);

    Map<TermId, List<HpoFrequency>> getHpoTermCounts(List<HpoDisease> hpoDiseases);

    Map<TermId, Set<TermId>> getMaxoToHpoTermIdMap(Map<TermId, List<HpoFrequency>> hpoTermCounts);

    Map<TermId, List<DifferentialDiagnosis>> getMaxoTermToDifferentialDiagnosesMap(PpktSample sample,
                                                                                   DifferentialDiagnosisEngine engine,
                                                                                   Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                                                                                   Integer nDiseases,
                                                                                   BiometadataService biometadataService);
    HpoDiseases getHPOADiseases();

    RankMaxo getRankMaxo(List<DifferentialDiagnosis> allInitialDiagnoses,
                         List<DifferentialDiagnosis> initialDiagnoses,
                         DifferentialDiagnosisEngine engine,
                         Map<TermId, Set<TermId>> maxoToHpoTermIdMap);
}
