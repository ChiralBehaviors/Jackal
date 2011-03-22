package com.hellblazer.anubis.util;

public interface SampledWindow {
    int size();

    void sample(double sample);

    double value();
}
