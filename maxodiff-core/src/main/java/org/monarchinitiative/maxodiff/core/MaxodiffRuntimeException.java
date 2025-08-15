package org.monarchinitiative.maxodiff.core;

/**
 * Base runtime exception thrown by the Maxodiff functions.
 */
public class MaxodiffRuntimeException extends RuntimeException {

    public MaxodiffRuntimeException() {
        super();
    }

    public MaxodiffRuntimeException(String message) {
        super(message);
    }

    public MaxodiffRuntimeException(String message, Exception e) {
        super(message, e);
    }

    public MaxodiffRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaxodiffRuntimeException(Throwable cause) {
        super(cause);
    }

    protected MaxodiffRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
