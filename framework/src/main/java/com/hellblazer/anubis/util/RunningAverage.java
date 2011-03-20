package com.hellblazer.anubis.util;

public class RunningAverage implements SampledWindow {
    private int            count = 0;
    private int            head  = 0;
    private final double[] samples;
    private double         sum   = 0.0D;
    private int            tail  = 0;

    public RunningAverage(int windowSize) {
        samples = new double[windowSize];
    }

    @Override
    public boolean hasSamples() {
        return count != 0;
    }

    @Override
    public void sample(double sample) {
        sum += sample;
        if (count == samples.length) {
            sum -= removeFirst();
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

    private void addLast(double value) {
        samples[tail] = value;
        tail = (tail + 1) % samples.length;
        count++;
    }

    private double removeFirst() {
        double item = samples[head];
        count--;
        head = (head + 1) % samples.length;
        return item;
    }
}
