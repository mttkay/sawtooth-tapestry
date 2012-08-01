package com.github.kaeppler.sawtoothtapestry;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;

public class SawtoothWallpaper extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        // android.os.Debug.waitForDebugger();
        return new SawtoothEngine();
    }

    private class SawtoothEngine extends WallpaperService.Engine {

        private static final int FRAME_RATE = 25;

        private final Runnable drawFrame = new Runnable() {
            public void run() {
                drawFrame();
            }
        };

        private boolean visible;

        private Handler frameHandler;

        private Bitmap waveform;
        private BitmapDrawable background;

        private Paint waveformPaint;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            System.out.println("ENGINE: onCreate");

            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

            frameHandler = new Handler();

            waveformPaint = new Paint();

            background = (BitmapDrawable) getResources().getDrawable(R.drawable.background);
            background.setBounds(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);

            // TODO: load from API
            Bitmap rawWaveform = BitmapFactory.decodeResource(getResources(), R.drawable.waveform);
            waveform = new WaveformProcessor(SawtoothWallpaper.this).process(rawWaveform);

            waveformPaint.setAlpha(200);
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
        }

        private void drawWaveform(Canvas canvas) {
            canvas.drawBitmap(waveform, 0, 0, waveformPaint);
        }

        private void scheduleFrame() {
            frameHandler.postDelayed(drawFrame, 1000 / FRAME_RATE);
        }

        private void cancelScheduledFrame() {
            frameHandler.removeCallbacks(drawFrame);
        }
    }
}
