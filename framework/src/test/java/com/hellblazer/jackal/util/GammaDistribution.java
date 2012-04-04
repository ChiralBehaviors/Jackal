package com.hellblazer.jackal.util;

import java.util.Random;

public class GammaDistribution {
    private final Random random;
    private final double alpha;
    private final double beta;

    GammaDistribution(Random random, double alpha, double beta) {
        if (alpha <= 0 || beta <= 0) {
            throw new IllegalArgumentException(
                                               "alpha and beta must be strictly positive.");
        }
        this.random = random;
        this.alpha = alpha;
        this.beta = beta;
    }

    public synchronized double nextGamma() {
        double gamma = 0;
        if (alpha < 1) {
            double b, p;
            boolean flag = false;
            b = 1 + alpha * Math.exp(-1);
            while (!flag) {
                p = b * random.nextDouble();
                if (p > 1) {
                    gamma = -Math.log((b - p) / alpha);
                    if (random.nextDouble() <= Math.pow(gamma, alpha - 1)) {
                        flag = true;
                    }
                } else {
                    gamma = Math.pow(p, 1 / alpha);
                    if (random.nextDouble() <= Math.exp(-gamma)) {
                        flag = true;
                    }
                }
            }
        } else if (alpha == 1) {
            gamma = -Math.log(random.nextDouble());
        } else {
            double y = -Math.log(random.nextDouble());
            while (random.nextDouble() > Math.pow(y * Math.exp(1 - y),
                                                  alpha - 1)) {
                y = -Math.log(random.nextDouble());
            }
            gamma = alpha * y;
        }
        return beta * gamma;
    }

}
