package org.p2gx.maxodiff.core.mica;

import java.io.Serializable;

/**
 * High-performance flat structure for fast local serialization.
 * Storing data as parallel arrays completely eliminates object overhead.
 */
public record ResnikFlatData(
    String[] termsA,
    String[] termsB,
    double[] icMicas
) implements Serializable {
    private static final long serialVersionUID = 1L;
}


