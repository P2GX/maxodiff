package org.monarchinitiative.maxodiff.core.analysis;


/**
 * An exception thrown when data error (missing resource, invalid resource file, etc.) is detected.
 */
public class MaxodiffLiricalAnalysisException extends Exception {

    public MaxodiffLiricalAnalysisException() {
        super();
    }

    public MaxodiffLiricalAnalysisException(String message) {
        super(message);
    }

    public MaxodiffLiricalAnalysisException(String message, Exception e) {
        super(message, e);
    }

    public MaxodiffLiricalAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaxodiffLiricalAnalysisException(Throwable cause) {
        super(cause);
    }

    protected MaxodiffLiricalAnalysisException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
