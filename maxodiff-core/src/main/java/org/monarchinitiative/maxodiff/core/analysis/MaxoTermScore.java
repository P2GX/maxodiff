package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.List;
import java.util.Set;

//TODO replace nOmimTerms and nHPOTerms with respective array sizes
public record MaxoTermScore(String maxoId, int nOmimTerms, Set<TermId> omimTermIds, Set<TermId> maxoOmimTermIds,
                            int nHpoTerms, Set<TermId> hpoTermIds,
                            Double initialScore, Double score, Double scoreDiff,
                            TermId changedDiseaseId, List<DifferentialDiagnosis> maxoDiagnoses,
                            List<DifferentialDiagnosis> initialDiagnosesMaxoOrdered,
                            double[] originalCDF, double[] maxoTermCDF) {
}
