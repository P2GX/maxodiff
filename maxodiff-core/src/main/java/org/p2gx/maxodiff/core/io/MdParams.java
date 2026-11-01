package org.p2gx.maxodiff.core.io;

public record MdParams(int nRepetitions,
                       int nDiseases) {

    public static final int DEFAULT_N_REPEATS = 80;
    public static final int DEFAULT_N_DISEASES = 20;

    public MdParams {   // runs before the values are stored
        if (nRepetitions < 1) {
            throw new IllegalArgumentException("nRepetitions must be at least 1 but was " + nRepetitions);
        }
        if (nDiseases < 1) {
            throw new IllegalArgumentException("nDiseases must be at least 1 but was " + nDiseases);
        }
    }

    public static MdParams defaultParams() {
        return new MdParams(DEFAULT_N_REPEATS, DEFAULT_N_DISEASES);
    }
}
