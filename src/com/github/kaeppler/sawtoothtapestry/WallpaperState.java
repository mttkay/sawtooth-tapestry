package com.github.kaeppler.sawtoothtapestry;

import android.graphics.Matrix;
import android.view.animation.AnimationUtils;

class WallpaperState {

    boolean skipPendingFrame, isLoading;

    // some state variables that we have to keep to coordinate the waveform anim
    boolean animateLogo, bouncedLeft, bouncedRight;
    float lastDeltaX = 0.0f;


    // needed to keep track of the total duration of when the wallpaper was invisible,
    // since this time must be subtracted from currenAnimTimeMillis, or else the anim
    // will get jumpy when switching between visibility states
    long lastAnimTime, timePaused;

    void updateTimePaused() {
        timePaused += (AnimationUtils.currentAnimationTimeMillis() - lastAnimTime);
    }

    void updateLastAnimTime() {
        lastAnimTime = AnimationUtils.currentAnimationTimeMillis();
    }

    long currentAnimTime() {
        return AnimationUtils.currentAnimationTimeMillis() - timePaused;
    }

    void updateWaveformBounceState(Matrix matrix) {
        float[] values = new float[9]; // 3x3 matrix
        matrix.getValues(values);
        float dx = values[2]; // contains the translation along the X axis

        if (dx > lastDeltaX) {
            bouncedLeft = true;
        } else if (bouncedLeft && dx == 0) {
            bouncedRight = true;
        }

        if (bouncedLeft && bouncedRight) {
            bouncedLeft = bouncedRight = false;
            animateLogo = true;
        }

        lastDeltaX = dx;
    }
}
