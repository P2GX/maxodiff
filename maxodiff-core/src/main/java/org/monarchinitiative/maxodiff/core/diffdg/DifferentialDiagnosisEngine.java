package org.monarchinitiative.maxodiff.core.diffdg;

import org.monarchinitiative.maxodiff.core.model.DifferentialDiagnosis;
import org.monarchinitiative.maxodiff.core.model.Sample;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.List;

/**
 * The differential diagnosis engine represents a way for performing a differential diagnosis 
 * for a provided {@link Sample}, where signs and symptoms are encoded into terms of Human Phenotype Ontology.
 */
public interface DifferentialDiagnosisEngine {

    /**
     * Run the differential diagnosis on the provided <code>sample</code>.
     * 
     * @throws DifferentialDiagnosisEngineException upon any issues encountered in the analysis
     */
    List<DifferentialDiagnosis> run(Sample sample);

    /**
     * Run the differential diagnosis on the provided <code>sample</code> and <code>targetDiseases</code>.
     * The targetDiseases object is nullable.
     *
     * @throws DifferentialDiagnosisEngineException upon any issues encountered in the analysis
     */
    List<DifferentialDiagnosis> run(Sample sample, Collection<TermId> targetDiseases);

}
