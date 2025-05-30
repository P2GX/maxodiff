package org.monarchinitiative.maxodiff.core.analysis.refinement;

import org.monarchinitiative.maxodiff.core.analysis.HpoFrequency;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.RankMaxo;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO: insert description
 * Differential diagnosis results come from some source.
 * We don't expect to get differential diagnosis results for all possible diseases, e.g. the entire OMIM corpus.
 * We don't expect the collection of differential diagnoses to be in any particular order.
 * The MAxO terms are returned in unspecified order.
 */
public interface DiffDiagRefiner {

    RefinementResults run(Sample sample,
                          Collection<DifferentialDiagnosis> differentialDiagnoses,
                          RefinementOptions options,
                          RankMaxo rankMaxo,
                          Map<TermId, List<HpoFrequency>> hpoTermCounts,
                          Map<TermId, Set<TermId>> maxoToHpoTermIdMap
    );


    List<DifferentialDiagnosis> getOrderedDiagnoses(Collection<DifferentialDiagnosis> originalDifferentialDiagnoses,
                                                    RefinementOptions options);

    List<HpoDisease> getDiseases(List<DifferentialDiagnosis> differentialDiagnoses);

    Map<TermId, List<HpoFrequency>> getHpoTermCounts(List<HpoDisease> hpoDiseases);

    Map<TermId, Set<TermId>> getMaxoToHpoTermIdMap(List<TermId> termIdsToRemove,
                                                   Map<TermId, List<HpoFrequency>> hpoTermCounts);

    Map<TermId, List<DifferentialDiagnosis>> getMaxoTermToDifferentialDiagnosesMap(Sample sample,
                                                                                   DifferentialDiagnosisEngine engine,
                                                                                   Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                                                                                   Integer nDiseases);
    HpoDiseases getHPOADiseases();
}
