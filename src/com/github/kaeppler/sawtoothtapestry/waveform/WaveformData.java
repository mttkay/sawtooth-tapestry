package com.github.kaeppler.sawtoothtapestry.waveform;

public class WaveformData {

    public int width;
    public int height;
    int[] samples;

    public WaveformData(int width, int height, int[] samples) {
        this.width = width;
        this.height = height;
        this.samples = samples;
    }

    /**
     * @param requiredWidth the new width
     * @return the waveform data downsampled to the required width
     */
    public WaveformData scale(int requiredWidth) {
        if (requiredWidth <= 0) throw new IllegalArgumentException("Invalid width: " + requiredWidth);
        if (requiredWidth == samples.length) {
            return this;
        } else {
            int[] newSamples = new int[requiredWidth];
            int newMax = 0;
            for (int i = 0; i < requiredWidth; i++) {
                final int offset = (int) Math.floor(samples.length / (double) requiredWidth * i);
                newSamples[i] = samples[Math.min(samples.length - 1, offset)];
                if (newSamples[i] > newMax) {
                    newMax = newSamples[i];
                }
            }
            return new WaveformData(requiredWidth, newMax, newSamples);
        }
    }
}