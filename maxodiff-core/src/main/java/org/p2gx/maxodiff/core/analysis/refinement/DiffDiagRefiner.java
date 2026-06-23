package org.p2gx.maxodiff.core.analysis.refinement;

import org.p2gx.maxodiff.core.analysis.HpoFrequency;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResult;
import org.p2gx.maxodiff.core.analysis.RankedMaxoResultSingleDisease;
import org.p2gx.maxodiff.core.diffdg.DDxEngine;
import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;
import org.p2gx.maxodiff.core.model.PhenopacketData;
import org.p2gx.maxodiff.core.model.RankMaxo;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;

/**
 * Differential diagnosis results come from some source.
 * We don't expect to get differential diagnosis results for all possible diseases, e.g. the entire OMIM corpus.
 * We don't expect the collection of differential diagnoses to be in any particular order.
 * The MAxO terms are returned in unspecified order.
 */
public interface DiffDiagRefiner {

    List<RankedMaxoResult> run(PhenopacketData sample,
                               Set<TermId> initialDiagnosesIds,
                               RankMaxo rankMaxo,
                               int nThreads) throws Exception;

    List<RankedMaxoResultSingleDisease> runSingleDisease(PhenopacketData sample,
                                                         TermId targetDiseaseId,
                                                         Set<TermId> initialDiagnosesIds,
                                                         RankMaxo rankMaxo,
                                                         int nThreads) throws Exception;

    List<DifferentialDiagnosis> getOrderedDiagnoses(Collection<DifferentialDiagnosis> originalDifferentialDiagnoses);

    List<HpoDisease> getDiseases(List<DifferentialDiagnosis> differentialDiagnoses);

    List<HpoFrequency> getHpoFrequenciesNDiseases(List<HpoDisease> hpoDiseases, List<HpoFrequency> allHpoFrequencies);

    Map<TermId, Set<TermId>> getMaxoToHpoTermIdMap(List<HpoFrequency> hpoTermCounts);

    RankMaxo getRankMaxo(List<DifferentialDiagnosis> allInitialDiagnoses,
                         List<DifferentialDiagnosis> initialDiagnoses,
                         DDxEngine engine,
                         Map<TermId, Set<TermId>> maxoToHpoTermIdMap,
                         List<HpoFrequency> hpoFrequenciesNDiseases);







}
