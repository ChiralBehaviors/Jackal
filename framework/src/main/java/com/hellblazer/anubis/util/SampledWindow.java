package com.hellblazer.anubis.util;

public interface SampledWindow {
    boolean hasSamples();

    void sample(double sample);

    double value();
}
