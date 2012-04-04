package com.hellblazer.jackal.util;

public class RunningAverage extends Window implements SampledWindow {
    private double sum = 0.0D;

    public RunningAverage(int windowSize) {
        super(windowSize);
    }

    @Override
    public void sample(double sample) {
        sum += sample;
        if (count == samples.length) {
            double first = removeFirst();
            sum -= first;
        }
        addLast(sample);
    }

    @Override
    public double value() {
        if (count == 0) {
            throw new IllegalStateException(
                                            "Must have at least one sample to calculate the average");
        }
        return sum / count;
    }
}
