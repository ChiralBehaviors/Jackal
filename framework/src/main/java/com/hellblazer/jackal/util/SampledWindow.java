package com.hellblazer.jackal.util;

public interface SampledWindow {
    void sample(double sample);

    int size();

    double value();
}
