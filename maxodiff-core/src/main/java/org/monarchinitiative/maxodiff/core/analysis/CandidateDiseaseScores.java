package org.monarchinitiative.maxodiff.core.analysis;

import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.maxodiff.core.model.SamplePhenopacket;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CandidateDiseaseScores {

    private final Random random = new Random();
    private final MaxoHpoTermProbabilities maxoHpoTermProbabilities;

    public CandidateDiseaseScores(MaxoHpoTermProbabilities maxoHpoTermProbabilities) {
        this.maxoHpoTermProbabilities = maxoHpoTermProbabilities;
    }

    /**
     *
     * @param ppkt Input phenopacket with present and excluded HPO terms.
     * @param maxoId TermId of the MAxO term of interest.
     * @param engine Engine to use for the differential diagnosis, e.g. LIRICAL.
     * @return List of differential diagnosis scores for the given MAxO term.
     */
    public List<Double> getScoresForMaxoTerm(SamplePhenopacket ppkt, TermId maxoId, DifferentialDiagnosisEngine engine) {
        Set<TermId> observed = new HashSet<>(Set.of());
        Set<TermId> excluded = new HashSet<>(Set.of());

        Set<TermId> maxoBenefitHpoIds = maxoHpoTermProbabilities.getMaxoTermBenefitIds(ppkt, maxoId);
        for (TermId hpoId : maxoBenefitHpoIds) {
            double maxoTermBenefitProbability = maxoHpoTermProbabilities.calculateProbabilityOfMaxoTermRevealingPresenceOfHpoTerm(hpoId);
            boolean result = getTestResult(maxoTermBenefitProbability);
            if (result) {
                observed.add(hpoId);
            } else {
                excluded.add(hpoId);
            }
        }

        Sample newSample = getNewSample(ppkt, observed, excluded);
        List<DifferentialDiagnosis> maxoDiagnoses = engine.run(newSample);
        //TODO: investigate ordering
        List<DifferentialDiagnosis> orderedMaxoDiagnoses = maxoDiagnoses.stream()
                .sorted(Comparator.comparingDouble(DifferentialDiagnosis::score).reversed())
                .toList();

        return orderedMaxoDiagnoses.stream().map(DifferentialDiagnosis::score).toList()
                .subList(0, maxoHpoTermProbabilities.nDiseases());
    }

    private boolean getTestResult(double maxoTermBenefitProbability) {
        float randomNumber = random.nextFloat();

        return randomNumber > maxoTermBenefitProbability;
    }

    private Sample getNewSample(SamplePhenopacket ppkt, Set<TermId> observed, Set<TermId> excluded) {
        Set<TermId> ppktObserved = new HashSet<>(ppkt.presentHpoTermIds());
        Set<TermId> ppktExcluded = new HashSet<>(ppkt.excludedHpoTermIds());
        Set<TermId> newObserved = Stream.concat(ppktObserved.stream(), observed.stream()).collect(Collectors.toSet());
        Set<TermId> newExcluded = Stream.concat(ppktExcluded.stream(), excluded.stream()).collect(Collectors.toSet());

        return Sample.of(ppkt.id(), newObserved, newExcluded);
    }
}
