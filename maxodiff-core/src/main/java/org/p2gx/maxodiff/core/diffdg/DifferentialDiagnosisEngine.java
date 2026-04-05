package org.p2gx.maxodiff.core.diffdg;

import org.p2gx.maxodiff.core.model.DifferentialDiagnosis;
import org.p2gx.maxodiff.core.model.PhenopacketData;
import org.p2gx.maxodiff.core.model.PpktSample;
import org.monarchinitiative.phenol.ontology.data.TermId;

import java.util.Collection;
import java.util.List;

/**
 * The differential diagnosis engine represents a way for performing a differential diagnosis 
 * for a provided {@link PpktSample}, where signs and symptoms are encoded into terms of Human Phenotype Ontology.
 */
public interface DifferentialDiagnosisEngine {

    /**
     * Run the differential diagnosis on the provided <code>sample</code>.
     * 
     * @throws DifferentialDiagnosisEngineException upon any issues encountered in the analysis
     */
    List<DifferentialDiagnosis> run(PhenopacketData sample);

    /**
     * Run the differential diagnosis on the provided <code>sample</code> and <code>targetDiseases</code>.
     * The targetDiseases object is nullable.
     *
     * @throws DifferentialDiagnosisEngineException upon any issues encountered in the analysis
     */
    List<DifferentialDiagnosis> run(PhenopacketData sample, Collection<TermId> targetDiseases);

}
