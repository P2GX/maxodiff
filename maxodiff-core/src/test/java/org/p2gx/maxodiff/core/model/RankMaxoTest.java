package org.p2gx.maxodiff.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankMaxoTest {

    @Test
    void thePoolIsTheHyperparameterWhenEnoughDiseasesAreAvailable() {
        assertEquals(RankMaxo.N_SAMPLING_DIAGNOSES, RankMaxo.samplingPoolSize(8000));
        assertEquals(RankMaxo.N_SAMPLING_DIAGNOSES, RankMaxo.samplingPoolSize(RankMaxo.N_SAMPLING_DIAGNOSES));
    }

    @Test
    void thePoolIsReducedWhenFewerDiseasesAreAvailable() {
        assertEquals(120, RankMaxo.samplingPoolSize(120));
        assertEquals(0, RankMaxo.samplingPoolSize(0));
    }
}