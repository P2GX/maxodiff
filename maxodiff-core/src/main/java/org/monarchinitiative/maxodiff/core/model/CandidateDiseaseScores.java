package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.maxodiff.core.SimpleTerm;
import org.monarchinitiative.maxodiff.core.analysis.MaxoHpoTermIdMaps;
import org.monarchinitiative.maxodiff.core.diffdg.DifferentialDiagnosisEngine;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CandidateDiseaseScores {

    private final Random random = new Random();
    private final MaxoHpoTermProbabilities maxoHpoTermProbabilities; //contains top K initial diagnoses only

    public CandidateDiseaseScores(MaxoHpoTermProbabilities maxoHpoTermProbabilities) {
        this.maxoHpoTermProbabilities = maxoHpoTermProbabilities;
    }

    /**
     *
     * @param ppkt Input phenopacket with present and excluded HPO terms.
     * @param maxoId TermId of the MAxO term of interest.
     * @param engine Engine to use for the differential diagnosis, e.g. LIRICAL.
     * @return List of the top K differential diagnoses for the given MAxO term.
     */
    public List<DifferentialDiagnosis> getScoresForMaxoTerm(Sample ppkt, TermId maxoId,
                                                            DifferentialDiagnosisEngine engine,
                                                            Set<TermId> diseaseIds,
                                                            Map<SimpleTerm, Set<SimpleTerm>> hpoToMaxoTermMap) {
        Set<TermId> observed = new HashSet<>(Set.of());
        Set<TermId> excluded = new HashSet<>(Set.of());

        Map<TermId, Set<TermId>> maxoToHpoTermIdMap = MaxoHpoTermIdMaps.getMaxoToHpoTermIdMap(hpoToMaxoTermMap);
        Set<TermId> maxoBenefitHpoIds = maxoHpoTermProbabilities.getDiscoverableByMaxoHpoTerms(ppkt, maxoId, maxoToHpoTermIdMap);
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

        return engine.run(newSample, diseaseIds);
    }

    private boolean getTestResult(double maxoTermBenefitProbability) {
        float randomNumber = random.nextFloat();

        return randomNumber > maxoTermBenefitProbability;
    }

    private Sample getNewSample(Sample ppkt, Set<TermId> observed, Set<TermId> excluded) {
        Set<TermId> ppktObserved = new HashSet<>(ppkt.presentHpoTermIds());
        Set<TermId> ppktExcluded = new HashSet<>(ppkt.excludedHpoTermIds());
        Set<TermId> newObserved = Stream.concat(ppktObserved.stream(), observed.stream()).collect(Collectors.toSet());
        Set<TermId> newExcluded = Stream.concat(ppktExcluded.stream(), excluded.stream()).collect(Collectors.toSet());

        return Sample.of(ppkt.id(), newObserved, newExcluded);
    }
}
