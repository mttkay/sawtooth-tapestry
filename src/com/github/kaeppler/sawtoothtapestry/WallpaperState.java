package com.github.kaeppler.sawtoothtapestry;

import android.view.animation.AnimationUtils;

class WallpaperState {

    // some state variables that we have to keep to coordinate the waveform anim
    boolean renderWaveform, animateLogo, skipPendingFrame;
    boolean bouncedLeft, bouncedRight;
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

}
