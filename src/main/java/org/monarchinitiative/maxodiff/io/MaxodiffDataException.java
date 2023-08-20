package org.monarchinitiative.maxodiff.io;


/**
 * An exception thrown when data error (missing resource, invalid resource file, etc.) is detected.
 */
public class MaxodiffDataException extends Exception {

    public MaxodiffDataException() {
        super();
    }

    public MaxodiffDataException(String message) {
        super(message);
    }

    public MaxodiffDataException(String message, Exception e) {
        super(message, e);
    }

    public MaxodiffDataException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaxodiffDataException(Throwable cause) {
        super(cause);
    }

    protected MaxodiffDataException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
