package com.github.kaeppler.sawtoothtapestry;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;

import com.github.kaeppler.sawtoothtapestry.animation.Flip3dAnimation;
import com.github.kaeppler.sawtoothtapestry.animation.Flip3dAnimationListener;

public class SawtoothWallpaper extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        // android.os.Debug.waitForDebugger();
        return new SawtoothEngine();
    }

    private class SawtoothEngine extends WallpaperService.Engine implements Flip3dAnimationListener {

        private static final int SECOND = 1000;
        private static final int FRAME_RATE = 60;

        private final Runnable drawFrame = new Runnable() {
            public void run() {
                drawFrame();
            }
        };

        private boolean visible;

        private Handler frameHandler;

        private DisplayMetrics displayMetrics;

        private Bitmap waveform, logoFrontSide, logoFlipSide, logoCurrentSide;
        private Drawable background, centerPiece, separatorTop, separatorBottom;

        private Paint waveformPaint;

        private AnimationSet waveformAnim;
        private Transformation waveformTransformation, logoTransformation;

        // for animating the 3D flip of the SC logo
        private Animation soundCloudLogoAnim;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            System.out.println("ENGINE: onCreate");

            displayMetrics = getResources().getDisplayMetrics();

            frameHandler = new Handler();

            waveformPaint = new Paint();

            setupBackground();

            // TODO: load from API
            Bitmap rawWaveform = BitmapFactory.decodeResource(getResources(), R.drawable.waveform);
            waveform = new WaveformProcessor(SawtoothWallpaper.this).process(rawWaveform);

            waveformPaint.setAlpha(100);
            waveformTransformation = new Transformation();

            buildWaveformAnimation(displayMetrics);
        }

        private void setupBackground() {
            Resources resources = getResources();

            background = resources.getDrawable(R.drawable.background);
            background.setBounds(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);

            centerPiece = resources.getDrawable(R.drawable.center_piece);
            int centerPieceHeight = (int) resources.getDimension(R.dimen.center_piece_height);
            int centerPieceY = displayMetrics.heightPixels / 2 - centerPieceHeight / 2;
            centerPiece.setBounds(0, centerPieceY, displayMetrics.widthPixels, centerPieceY
                    + centerPieceHeight);

            separatorTop = resources.getDrawable(R.drawable.separator_line);
            int separatorHeight = (int) resources.getDimension(R.dimen.separator_height);
            separatorTop.setBounds(0, centerPieceY - separatorHeight, displayMetrics.widthPixels,
                    centerPieceY);

            separatorBottom = resources.getDrawable(R.drawable.separator_line);
            separatorBottom.setBounds(0, centerPieceY + centerPieceHeight,
                    displayMetrics.widthPixels, centerPieceY + centerPieceHeight + separatorHeight);

            // set up the SC logo plus animation
            logoFrontSide = BitmapFactory.decodeResource(resources, R.drawable.soundcloud_logo);
            logoFlipSide = BitmapFactory.decodeResource(resources,
                    R.drawable.soundcloud_logo_loading);
            logoCurrentSide = logoFrontSide;
            soundCloudLogoAnim = new Flip3dAnimation(this);
            soundCloudLogoAnim.setRepeatCount(Animation.INFINITE);
            soundCloudLogoAnim.setRepeatMode(Animation.REVERSE);
            soundCloudLogoAnim.setStartTime(Animation.START_ON_FIRST_FRAME);
            soundCloudLogoAnim.initialize(logoCurrentSide.getWidth(), logoCurrentSide.getHeight(),
                    logoCurrentSide.getWidth(), logoCurrentSide.getHeight());
            logoTransformation = new Transformation();
        }

        private void buildWaveformAnimation(DisplayMetrics displayMetrics) {
            waveformAnim = new AnimationSet(true);

            // moves the waveform back and forth horizontally across the screen
            Animation scrollAnim = new TranslateAnimation(Animation.ABSOLUTE, 0,
                    Animation.ABSOLUTE, displayMetrics.widthPixels - waveform.getWidth(),
                    Animation.ABSOLUTE, 0, Animation.ABSOLUTE, 0);
            scrollAnim.setInterpolator(new LinearInterpolator());
            initializeAnimation(scrollAnim, 20 * SECOND);

            // grows and shrinks the waveform vertically using a bounce effect
            Animation growAnim = new ScaleAnimation(1.0f, 1.0f, 0.0f, 1.0f, Animation.ABSOLUTE,
                    displayMetrics.widthPixels / 2.0f, Animation.ABSOLUTE,
                    waveform.getHeight() / 2.0f);
            growAnim.setInterpolator(new BounceInterpolator());
            initializeAnimation(growAnim, 10 * SECOND);

            waveformAnim.addAnimation(scrollAnim);
            waveformAnim.addAnimation(growAnim);
            waveformAnim.setStartTime(Animation.START_ON_FIRST_FRAME);
        }

        private void initializeAnimation(Animation animation, long duration) {
            animation.setDuration(duration);
            animation.setFillAfter(true);
            animation.setRepeatMode(Animation.REVERSE);
            animation.setRepeatCount(Animation.INFINITE);
            animation.initialize(waveform.getWidth(), waveform.getHeight(), waveform.getWidth(),
                    waveform.getHeight());
        }

        @Override
        public void onDestroy() {
            // TODO Auto-generated method stub
            super.onDestroy();
            System.out.println("ENGINE: onDestroy");

            cancelScheduledFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            // TODO Auto-generated method stub
            super.onSurfaceCreated(holder);
            System.out.println("ENGINE: onSurfaceCreated");
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // TODO Auto-generated method stub
            super.onSurfaceChanged(holder, format, width, height);
            System.out.println("ENGINE: onSurfaceChanged");

            // TODO: adjust to changes

            drawFrame();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            // TODO Auto-generated method stub
            super.onSurfaceDestroyed(holder);
            System.out.println("ENGINE: onSurfaceDestroyed");

            visible = false;
            cancelScheduledFrame();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            System.out.println("ENGINE: onVisibilityChanged -> " + visible);
            this.visible = visible;
            if (visible) {
                drawFrame();
            } else {
                cancelScheduledFrame();
            }
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep,
                float yOffsetStep, int xPixelOffset, int yPixelOffset) {
            super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep, xPixelOffset,
                    yPixelOffset);
            System.out.println("ENGINE: onOffsetsChanged");
        }

        private void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();

            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    drawBackground(canvas);
                    drawWaveform(canvas);
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            cancelScheduledFrame();
            if (visible) {
                scheduleFrame();
            }
        }

        private void drawBackground(Canvas canvas) {
            background.draw(canvas);
            centerPiece.draw(canvas);
            separatorTop.draw(canvas);
            separatorBottom.draw(canvas);

            // animate the SoundCloud logo
            soundCloudLogoAnim.getTransformation(AnimationUtils.currentAnimationTimeMillis(),
                    logoTransformation);
            logoTransformation.getMatrix().postTranslate(
                    displayMetrics.widthPixels / 2 - logoCurrentSide.getWidth() / 2,
                    displayMetrics.heightPixels / 2 - logoCurrentSide.getHeight() / 2);
            canvas.drawBitmap(logoCurrentSide, logoTransformation.getMatrix(), null);
        }

        private void drawWaveform(Canvas canvas) {
            // compute the animation progress for this frame
            waveformAnim.getTransformation(AnimationUtils.currentAnimationTimeMillis(),
                    waveformTransformation);

            // move the bitmap to the vertical center of the screen
            waveformTransformation.getMatrix().postTranslate(0,
                    displayMetrics.heightPixels / 2 - waveform.getHeight() / 2);

            canvas.drawBitmap(waveform, waveformTransformation.getMatrix(), waveformPaint);
        }

        private void scheduleFrame() {
            frameHandler.postDelayed(drawFrame, 1000 / FRAME_RATE);
        }

        private void cancelScheduledFrame() {
            frameHandler.removeCallbacks(drawFrame);
        }

        @Override
        public void onFrontSideVisible() {
            this.logoCurrentSide = logoFrontSide;
        }

        @Override
        public void onFlipSideVisible() {
            this.logoCurrentSide = logoFlipSide;
        }
    }
}
