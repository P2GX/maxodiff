package org.monarchinitiative.maxodiff.core.model;

import org.monarchinitiative.phenol.ontology.data.TermId;
import java.util.HashMap;
import java.util.Map;

/**
 * There are some diagnostic MAxO terms that are used as grouping terms. Sometimes, they have been used in the MAXO annotations
 * to denote that a more specific diagnosic term was not available. For the purposes of MAxO-diff, we want to skip
 * these terms. For instance, we have used "medical action" as an annotation to show that no other term is available.
 * We do not expect this list to change much, if at all, in the future.
 * @author peter robinson
 */
public class GeneralMaxoTerms {
    private static TermId clinicalAssessment = TermId.of("MAXO:0000487");
    private static TermId medicalAction = TermId.of("MAXO:0000001");
    private static TermId medicalHistoryTaking = TermId.of("MAXO:0000574");
    private static TermId magneticResonanceImagingProcedure = TermId.of("MAXO:0000424");
    private static TermId clinicalBiopsy = TermId.of("MAXO:0001269");
    private static TermId clinicalCoreNeedleBiopsy = TermId.of("MAXO:0001271");
    private static Map<TermId, String> generalMaxoTerms = null;

    public static Map<TermId, String> getGeneralMaxoTerms() {
        if (generalMaxoTerms == null) {
            generalMaxoTerms = new HashMap<>();

            generalMaxoTerms.put(clinicalAssessment, "clinical assessment");
            generalMaxoTerms.put(medicalAction, "medical action");
            generalMaxoTerms.put(medicalHistoryTaking, "medical history taking");
            generalMaxoTerms.put(magneticResonanceImagingProcedure, "magnetic resonance imaging procedure");
            generalMaxoTerms.put(clinicalBiopsy, "clinical biopsy");
            generalMaxoTerms.put(clinicalCoreNeedleBiopsy, "clinical core needle biopsy");
        }
        return generalMaxoTerms;
    }
}
