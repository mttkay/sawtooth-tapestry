package com.github.kaeppler.sawtoothtapestry;

import android.view.animation.AnimationUtils;

class WallpaperState {

    enum PlayerState {
        IDLE, PLAYING, PAUSED;

        public static PlayerState fromString(String in) {
            if ("PLAYING".equalsIgnoreCase(in)) {
                return PLAYING;
            } else if ("PAUSED".equalsIgnoreCase(in)) {
                return PAUSED;
            } else {
                return IDLE;
            }
        }
    }
    PlayerState playerState;

    boolean isLoading;

    // some state variables that we have to keep to coordinate the waveform anim
    boolean animateLogo, bouncedLeft, bouncedRight;

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
