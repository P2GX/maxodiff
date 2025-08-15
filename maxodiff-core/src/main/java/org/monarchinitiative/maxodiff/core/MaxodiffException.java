package org.monarchinitiative.maxodiff.core;

/**
 * Base checked exception thrown by the Maxodiff functions.
 */
public class MaxodiffException extends Exception {

    public MaxodiffException() {
        super();
    }

    public MaxodiffException(String message) {
        super(message);
    }

    public MaxodiffException(String message, Exception e) {
        super(message, e);
    }

    public MaxodiffException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaxodiffException(Throwable cause) {
        super(cause);
    }

    protected MaxodiffException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
