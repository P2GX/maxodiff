package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Set;

public record MaxoTermScore(String maxoId, Integer nOmimTerms, Set<TermId> omimTermIds, Set<TermId> maxoOmimTermIds,
                            Integer nHpoTerms, Set<TermId> hpoTermIds,
                            Double initialScore, Double score, Double scoreDiff,
                            TermId changedDiseaseId, List<DifferentialDiagnosis> maxoDiagnoses,
                            List<DifferentialDiagnosis> initialDiagnosesMaxoOrdered,
                            double[] originalCDF, double[] maxoTermCDF) {
}
