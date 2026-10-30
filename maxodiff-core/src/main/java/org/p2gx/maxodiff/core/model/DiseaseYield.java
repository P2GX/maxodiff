package org.p2gx.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * What performing one MAXO (diagnostic) test could reveal about one candidate disease.
 *
 * @param diagnosis      the candidate disease from the Phenomizer differential diagnosis
 * @param rank           1-based position of the disease in the differential (1 = most likely)
 * @param revealedByMaxo HPO terms annotated to the disease that the test can detect and that are
 *                       not already observed or excluded in the patient
 */
public record DiseaseYield(DifferentialDiagnosis diagnosis, int rank, Set<TermId> revealedByMaxo) {

    public DiseaseYield {
        Objects.requireNonNull(diagnosis, "diagnosis must not be null");
        Objects.requireNonNull(revealedByMaxo, "revealedByMaxo must not be null");
        if (rank < 1) {
            throw new IllegalArgumentException("rank must be >= 1 but was " + rank);
        }
        revealedByMaxo = Set.copyOf(revealedByMaxo); // immutable defensive copy
    }

    /**
     * @param diagnosis   candidate disease
     * @param rank        1-based rank of the disease in the differential
     * @param assessable  the disease's HPO terms that are not yet in the patient
     * @param maxoHpoIds  all HPO terms the MAXO test can detect
     */
    public static DiseaseYield of(DifferentialDiagnosis diagnosis, int rank,
                                  Set<TermId> assessable, Set<TermId> maxoHpoIds) {
        Set<TermId> revealed = new HashSet<>(assessable); // copy: never mutate the caller's set
        revealed.retainAll(maxoHpoIds);
        return new DiseaseYield(diagnosis, rank, revealed);
    }

    /** How much we believe this disease: 1, 1/2, 1/3, ... */
    public double weight() {
        return 1.0 / rank;
    }

    /** Number of HPO terms the test could reveal for this disease. */
    public int count() {
        return revealedByMaxo.size();
    }
}