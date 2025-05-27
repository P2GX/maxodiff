package org.monarchinitiative.maxodiff.phenomizer;

import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDisease;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseaseAnnotation;
import org.monarchinitiative.phenol.annotations.formats.hpo.HpoDiseases;
import org.monarchinitiative.phenol.ontology.data.TermId;
import org.monarchinitiative.phenol.ontology.similarity.TermPair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PhenomizerDifferentialDiagnosisEngine implements DifferentialDiagnosisEngine {

    private final HpoDiseases diseases;
    private final Map<TermPair, Double> termPairToIc;
    private final Map<TermId, Integer> diseaseToPresentAnnotationCount;
    private final ScoringMode scoringMode;

    public PhenomizerDifferentialDiagnosisEngine(
            HpoDiseases diseases,
            Map<TermPair, Double> termPairToIc,
            ScoringMode scoringMode
    ) {
        this.diseases = diseases;
        this.termPairToIc = termPairToIc;
        this.diseaseToPresentAnnotationCount = countPresentAnnotations(diseases);
        this.scoringMode = scoringMode;
    }

    private static Map<TermId, Integer> countPresentAnnotations(HpoDiseases diseases) {
        return diseases.stream().collect(Collectors.toUnmodifiableMap(HpoDisease::id, d -> Math.toIntExact(d.presentAnnotationsStream().count())));
    }

    private static double mean(double[] vals) {
        assert vals.length != 0;
        double sum = 0.;
        for (double val : vals)
            sum += val;

        return sum / vals.length;
    }

    @Override
    public List<DifferentialDiagnosis> run(Sample sample) {
        return run(sample, null);
    }

    @Override
    public List<DifferentialDiagnosis> run(Sample sample, Collection<TermId> targetDiseases) {
        int nDiag = targetDiseases == null ? diseases.size() : targetDiseases.size();
        List<DifferentialDiagnosis> diagnoses = new ArrayList<>(nDiag);

        for (HpoDisease disease : diseases) {
            if (targetDiseases != null && !targetDiseases.contains(disease.id()))
                continue;

            double similarity = switch (scoringMode) {
                case ONE_SIDED -> oneSided(
                        sample.presentHpoTermIds(),
                        disease
                );
                case TWO_SIDED -> twoSided(
                        sample.presentHpoTermIds(),
                        disease
                );
            };

            diagnoses.add(DifferentialDiagnosis.of(
                    disease.id(),
                    similarity,
                    Double.NaN
            ));
        }

        return diagnoses;
    }

    private double oneSided(
            Collection<TermId> query,
            HpoDisease disease
    ) {
        int presentAnnotationCount = diseaseToPresentAnnotationCount.get(disease.id());
        if (query.isEmpty() || presentAnnotationCount == 0)
            return 0.;

        double[] vals = new double[query.size()];
        int i = 0;
        for (TermId q : query) {
            for (HpoDiseaseAnnotation anno : disease.presentAnnotations()) {
                TermPair pair = TermPair.symmetric(q, anno.id());
                Double icMica = termPairToIc.getOrDefault(pair, 0.);
                vals[i] = Double.max(icMica, vals[i]);
            }
            i++;
        }

        return mean(vals);
    }

    private double twoSided(
            Collection<TermId> query,
            HpoDisease disease
    ) {
        int presentAnnotationCount = diseaseToPresentAnnotationCount.get(disease.id());
        if (query.isEmpty() || presentAnnotationCount == 0)
            return 0.;

        double[] queryToDisease = new double[query.size()];
        double[] diseaseToQuery = new double[presentAnnotationCount];
        int q = 0;
        for (TermId feature : query) {
            double q2d = 0;
            int d = 0;
            for (HpoDiseaseAnnotation anno : disease.presentAnnotations()) {
                TermPair pair = TermPair.symmetric(feature, anno.id());
                double icMica = termPairToIc.getOrDefault(pair, 0.);

                q2d = Double.max(icMica, q2d);
                diseaseToQuery[d] = Double.max(icMica, diseaseToQuery[d]);

                d++;
            }
            queryToDisease[q] = q2d;
            q++;
        }

        return (mean(queryToDisease) + mean(diseaseToQuery)) * .5;
    }
}
