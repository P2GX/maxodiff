package org.monarchinitiative.maxodiff.core.model;

import java.util.Random;

public class PoissonSampler {

    private static Random rng = new Random();

    public PoissonSampler() {    }

    public PoissonSampler(long seed) {
        rng = new Random(seed);
    }

    /**
     * Sample from a Poisson distribution with mean lambda.
     */
    public static int sample(double lambda) {
        if (lambda <= 0) {
            return 0;
        }

        double L = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;

        do {
            k++;
            p *= rng.nextDouble();
        } while (p > L);

        return k - 1;
    }

    public static void main(String[] args) {
        PoissonSampler sampler = new PoissonSampler();

        double mean = 3.2;
        for (int i = 0; i < 20; i++) {
            int sample = PoissonSampler.sample(mean);
            System.out.println(sample);
        }
    }

}
