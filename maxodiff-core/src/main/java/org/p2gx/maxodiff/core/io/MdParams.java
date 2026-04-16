package org.p2gx.maxodiff.core.io;

public record MdParams(int nRepetitions,
                       int nDiseases) {

    public static final int DEFAULT_N_REPEATS = 100;
    public static final int DEFAULT_N_DISEASES = 20;

    public static MdParams defaultParams() {
        return new MdParams(DEFAULT_N_REPEATS, DEFAULT_N_DISEASES);
    }
}
