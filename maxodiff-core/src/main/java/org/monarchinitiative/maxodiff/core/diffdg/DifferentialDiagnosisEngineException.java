package org.monarchinitiative.maxodiff.core.diffdg;

import org.monarchinitiative.maxodiff.core.MaxodiffRuntimeException;

/**
 * An exception thrown for issues encountered by {@link DifferentialDiagnosisEngine}.
 */
public class DifferentialDiagnosisEngineException extends MaxodiffRuntimeException {

    public DifferentialDiagnosisEngineException() {
        super();
    }

    public DifferentialDiagnosisEngineException(String message) {
        super(message);
    }

    public DifferentialDiagnosisEngineException(String message, Exception e) {
        super(message, e);
    }

    public DifferentialDiagnosisEngineException(String message, Throwable cause) {
        super(message, cause);
    }

    public DifferentialDiagnosisEngineException(Throwable cause) {
        super(cause);
    }

    protected DifferentialDiagnosisEngineException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
