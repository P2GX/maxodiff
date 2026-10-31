package org.p2gx.maxodiff.html.config;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when all analysis slots stayed taken for the whole waiting time.
 * Spring turns it into an HTTP 503. */
@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE,
        reason = "The server is busy with other analyses. Please try again in a few minutes.")
public class ServerBusyException extends RuntimeException {
    public ServerBusyException() {
        super("All analysis slots are in use");
    }
}