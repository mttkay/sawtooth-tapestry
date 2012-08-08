package com.github.kaeppler.sawtoothtapestry.animation;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class Flip3dAnimation extends Animation {
    public static final int MIN_DEGREES = 0;
    public static final int MAX_DEGREES = 180;
    public static final int X = 0;
    public static final int Y = 1;
    public static final int Z = 2;

    private float fromDegrees;
    private float toDegrees;
    private float centerX;
    private float centerY;
    private int axis;
    private Camera camera;
    private boolean flipped = false;
    private boolean counterClockwise = false;

    private Flip3dAnimationListener listener;

    public Flip3dAnimation(Flip3dAnimationListener listener) {
        this.listener = listener;
        this.axis = Y;

        setInterpolator(new AccelerateInterpolator());
        setDuration(1000);
        setFillAfter(true);
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        fromDegrees = MIN_DEGREES;
        toDegrees = MAX_DEGREES;
        camera = new Camera();
        this.centerX = width / 2.0f;
        this.centerY = width / 2.0f;
        this.flipped = false;
    }

    public void setVisibleSide(float degrees) {
        if (!flipped && degrees >= MAX_DEGREES / 2) {
            // At 90deg, when the image is half-way flipped, we do a 0-time spin by another 180deg,
            // so as to ensure that only ever the front side of the view is visible. That's because
            // otherwise the flipside would appear mirrored, because technically, a view in Android
            // does not have a backside.
            toDegrees += MAX_DEGREES;
            fromDegrees += MAX_DEGREES;
            // set the back side to be the new front side
            flipped = true;
            listener.onFlipSideVisible();
        } else if (flipped && degrees <= toDegrees - MAX_DEGREES / 2) {
            // reverse the flip (see above)
            toDegrees -= MAX_DEGREES;
            fromDegrees -= MAX_DEGREES;
            flipped = false;
            listener.onFrontSideVisible();
        }
    }

    public void setAxis(int axis) {
        this.axis = axis;
    }

    public void setDirection(boolean counterClockwise) {
        this.counterClockwise = counterClockwise;
    }

    public boolean isFlipped() {
        return flipped;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float degrees = fromDegrees + ((toDegrees - fromDegrees) * interpolatedTime);

        if (this.hasEnded()) {
            return;
        }

        if (counterClockwise) {
            degrees *= -1;
        }

        setVisibleSide(degrees);

        camera.save();

        if (axis == X) {
            camera.rotateX(degrees);
        } else if (axis == Y) {
            camera.rotateY(degrees);
        } else {
            camera.rotateZ(degrees);
        }

        final Matrix matrix = t.getMatrix();
        camera.getMatrix(matrix);

        camera.restore();

        matrix.preTranslate(-centerX, -centerY);
        matrix.postTranslate(centerX, centerY);
    }

}
