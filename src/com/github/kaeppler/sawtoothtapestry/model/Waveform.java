package com.github.kaeppler.sawtoothtapestry.model;

import com.github.kaeppler.sawtoothtapestry.waveform.SampleData;

import android.graphics.Bitmap;

public class Waveform {

    private SampleData sampleData;
    private Bitmap bitmap;

    public Waveform(SampleData sampleData, Bitmap bitmap) {
        this.sampleData = sampleData;
        this.bitmap = bitmap;
    }

    public SampleData getSampleData() {
        return sampleData;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getWidth() {
        return bitmap.getWidth();
    }

    public int getHeight() {
        return bitmap.getHeight();
    }
}
