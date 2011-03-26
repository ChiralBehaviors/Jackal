package com.hellblazer.jackal.util;

public interface SampledWindow {
    int size();

    void sample(double sample);

    double value();
}
