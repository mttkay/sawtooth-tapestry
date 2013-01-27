package com.github.kaeppler.sawtoothtapestry;

import com.github.kaeppler.sawtoothtapestry.animation.Flip3dAnimation;
import com.github.kaeppler.sawtoothtapestry.animation.Flip3dAnimationListener;
import com.github.kaeppler.sawtoothtapestry.api.SoundCloudApi;
import com.github.kaeppler.sawtoothtapestry.network.ConnectivityChangeBroadcastReceiver;
import com.github.kaeppler.sawtoothtapestry.network.NetworkListener;
import com.github.kaeppler.sawtoothtapestry.waveform.WaveformData;
import com.github.kaeppler.sawtoothtapestry.waveform.WaveformDownloader;
import com.github.kaeppler.sawtoothtapestry.waveform.WaveformProcessor;
import com.github.kaeppler.sawtoothtapestry.waveform.WaveformUrlManager;
import com.integralblue.httpresponsecache.HttpResponseCache;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Message;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;

public class SawtoothWallpaper extends WallpaperService {

    public static final String ACTION_SETTINGS_CHANGED = "sc_wallpaper_settings_changed";

    private static final String TAG = SawtoothWallpaper.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate (service)");
    }

    @Override
    public Engine onCreateEngine() {
        // android.os.Debug.waitForDebugger();
        Log.d(TAG, "onCreateEngine");
        return new SawtoothEngine();
    }

    private void handleNewWaveformsAvailable() {
        // TODO: this is currently unused, but could be useful at some point
        Log.d(TAG, "New waveforms available, smooth.");
    }

    private class SawtoothEngine extends WallpaperService.Engine implements
            Flip3dAnimationListener, Handler.Callback, NetworkListener {

        private static final int SECOND = 1000;
        private static final int FRAME_RATE = 60;
        private static final int HTTP_CACHE_SIZE = 1024 * 1024; // 1MB

        private WaveformUrlManager waveformManager;

        private BroadcastReceiver receiver;
        private Handler handler;

        private DisplayMetrics displayMetrics;

        private Bitmap waveform, logoFrontSide, logoFlipSide, logoCurrentSide;
        private Drawable background, centerPiece, separatorTop, separatorBottom;

        private Paint waveformPaint;

        private AnimationSet waveformAnim;
        private Flip3dAnimation soundCloudLogoAnim;
        private Transformation waveformTransformation, logoTransformation;

        private WaveformProcessor waveformProcessor;

        private WallpaperState state = new WallpaperState();

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            Log.d(TAG, "Engine: onCreate - preview = " + isPreview());

            getApplicationContext().registerReceiver(new ConnectivityChangeBroadcastReceiver(this),
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

            handler = new Handler(this);

            displayMetrics = getResources().getDisplayMetrics();

            waveformPaint = new Paint();
            waveformPaint.setAlpha(100);
            waveformProcessor = new WaveformProcessor(SawtoothWallpaper.this);
            waveformTransformation = new Transformation();

            setupBackground();

            SoundCloudApi api = new SoundCloudApi(SawtoothWallpaper.this);
            waveformManager = new WaveformUrlManager(SawtoothWallpaper.this, api, handler);

            if (!waveformManager.areWaveformsAvailable()) {
                SuperToast.info(getApplicationContext(), R.string.no_waveforms_available);
            }

            try {
                HttpResponseCache.install(getApplicationContext().getCacheDir(), HTTP_CACHE_SIZE);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Installing HTTP response cache failed");
            }

            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    System.out.println("COLOR CHANGE");
                    cancelScheduledFrame();
                    getNextWaveform();
                }
            };
            registerReceiver(receiver, new IntentFilter(ACTION_SETTINGS_CHANGED));
        }

        private void setupBackground() {
            Resources resources = getResources();

            background = resources.getDrawable(R.drawable.background);
            centerPiece = resources.getDrawable(R.drawable.center_piece);
            separatorTop = resources.getDrawable(R.drawable.separator_line);
            separatorBottom = resources.getDrawable(R.drawable.separator_line);

            // set up the SC logo plus animation
            logoFrontSide = BitmapFactory.decodeResource(resources, R.drawable.soundcloud_logo);
            logoFlipSide = BitmapFactory.decodeResource(resources,
                    R.drawable.soundcloud_logo_loading);
            logoCurrentSide = logoFrontSide;

            buildLogoAnimation();

            // initialize all bitmaps bounds based on the current display configuration
            updateBackground();
        }

        private void buildLogoAnimation() {
            soundCloudLogoAnim = new Flip3dAnimation(this);
            soundCloudLogoAnim.setRepeatCount(Animation.INFINITE);
            soundCloudLogoAnim.setRepeatMode(Animation.REVERSE);
            soundCloudLogoAnim.setStartTime(Animation.START_ON_FIRST_FRAME);
            soundCloudLogoAnim.initialize(logoCurrentSide.getWidth(), logoCurrentSide.getHeight(),
                    logoCurrentSide.getWidth(), logoCurrentSide.getHeight());
            soundCloudLogoAnim.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                    Log.d(TAG, "REPEAT logo anim (flipped: " + soundCloudLogoAnim.isFlipped() + ")");
                    state.animateLogo = false;
                    if (soundCloudLogoAnim.isFlipped()) {
                        // at this point, the LOADING side of the logo is visible; this is when
                        // we load the next waveform image
                        cancelScheduledFrame();
                        handler.sendEmptyMessage(R.id.message_download_waveform);
                    } else {
                        // otherwise, continue rendering the waveform image
                        state.renderWaveform = true;
                    }
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                }
            });
            logoTransformation = new Transformation();
        }

        private void updateBackground() {
            Resources resources = getResources();

            // update the background image bounds
            background.setBounds(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);

            // update the bounds of the box in the center of the screen
            int centerPieceHeight = (int) resources.getDimension(R.dimen.center_piece_height);
            int centerPieceY = displayMetrics.heightPixels / 2 - centerPieceHeight / 2;
            centerPiece.setBounds(0, centerPieceY, displayMetrics.widthPixels, centerPieceY
                    + centerPieceHeight);

            // update the separators that enclose the box
            int separatorHeight = (int) resources.getDimension(R.dimen.separator_height);
            separatorTop.setBounds(0, centerPieceY - separatorHeight, displayMetrics.widthPixels,
                    centerPieceY);
            separatorBottom.setBounds(0, centerPieceY + centerPieceHeight,
                    displayMetrics.widthPixels, centerPieceY + centerPieceHeight + separatorHeight);
        }

        private void buildWaveformAnimation() {
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
        public boolean handleMessage(Message msg) {

            switch (msg.what) {
            case R.id.message_download_waveform:
                getNextWaveform();
                break;
            case R.id.message_waveforms_available:
                handleNewWaveformsAvailable();
                break;
            case R.id.message_waveform_downloaded:
                handleNewWaveformDownloaded((WaveformData) msg.obj);
                break;
            }

            return true;
        }

        private void getNextWaveform() {
            waveform = null;
            waveformManager.refreshWaveformUrls();
            String nextWaveformUrl = waveformManager.getRandomWaveformUrl();
            Log.d(TAG, "Up next: " + nextWaveformUrl);
            new WaveformDownloader(nextWaveformUrl, handler).start();
        }

        private void handleNewWaveformDownloaded(WaveformData waveformData) {
            if (waveformData == null) {
                Log.e(TAG, "No bitmap received, is the network down?");
                return;
            }

            Log.d(TAG, "got waveform: " + waveformData.width + "x" + waveformData.height);

            waveform = waveformProcessor.process(waveformData);

            buildWaveformAnimation();

            // we're done loading the new image; flip the loading logo back to the front side
            state.animateLogo = true;

            // reset the flags
            state.bouncedLeft = state.bouncedRight = false;

            scheduleFrame();
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            Log.d(TAG, "Engine: onDestroy");

            cancelScheduledFrame();

            HttpResponseCache responseCache = HttpResponseCache.getInstalled();
            if (responseCache != null) {
                responseCache.flush();
            }

            if (receiver != null) {
                unregisterReceiver(receiver);
            }
        }

        // this is called e.g. when the screen orientation changes, since that will also change
        // the dimensions of the surface we draw to
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "Engine: onSurfaceChanged");
            super.onSurfaceChanged(holder, format, width, height);

            updateBackground();

            if (waveform != null) {
                // the waveform animation depends on the current screen width, so we need to rebuild
                // it whenever the surface changes bounds
                buildWaveformAnimation();

                scheduleFrame();
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "Engine: onSurfaceDestroyed");
            super.onSurfaceDestroyed(holder);
            cancelScheduledFrame();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            Log.d(TAG, "Engine: onVisibilityChanged: " + visible);

            if (visible) {
                state.updateTimePaused();
                Log.d(TAG, "anim pause: " + state.timePaused);
                scheduleFrame();
            } else {
                cancelScheduledFrame();
                state.updateLastAnimTime();
                Log.d(TAG, "last anim time: " + state.lastAnimTime);
            }
        }

        private void drawBackground(Canvas canvas) {
            background.draw(canvas);
            centerPiece.draw(canvas);
            separatorTop.draw(canvas);
            separatorBottom.draw(canvas);

            if (state.animateLogo) {
                // animate the SoundCloud logo
                soundCloudLogoAnim.getTransformation(AnimationUtils.currentAnimationTimeMillis(),
                        logoTransformation);
            } else {
                logoTransformation.getMatrix().reset();
            }
            logoTransformation.getMatrix().postTranslate(
                    displayMetrics.widthPixels / 2 - logoCurrentSide.getWidth() / 2,
                    displayMetrics.heightPixels / 2 - logoCurrentSide.getHeight() / 2);
            canvas.drawBitmap(logoCurrentSide, logoTransformation.getMatrix(), null);
        }

        private void drawWaveform(Canvas canvas) {
            // compute the animation progress for this frame
            waveformAnim.getTransformation(state.currentAnimTime(), waveformTransformation);

            // move the bitmap to the vertical center of the screen
            waveformTransformation.getMatrix().postTranslate(0,
                    displayMetrics.heightPixels / 2 - waveform.getHeight() / 2);

            float[] values = new float[9]; // 3x3 matrix
            waveformTransformation.getMatrix().getValues(values);
            // System.out.println(waveformTransformation.getMatrix().toShortString());
            float dx = values[2]; // contains the translation along the X axis

            canvas.drawBitmap(waveform, waveformTransformation.getMatrix(), waveformPaint);

            if (dx > state.lastDeltaX) {
                state.bouncedLeft = true;
            } else if (state.bouncedLeft && dx == 0) {
                state.bouncedRight = true;
            }

            if (state.bouncedLeft && state.bouncedRight) {
                state.bouncedLeft = state.bouncedRight = false;
                state.animateLogo = true;
                state.renderWaveform = false;
            }

            state.lastDeltaX = dx;
        }

        private final Runnable drawFrame = new Runnable() {
            public void run() {
                if (state.skipPendingFrame) {
                    Log.d(TAG, "<suppressed draw call>");
                    return;
                }

                final SurfaceHolder holder = getSurfaceHolder();

                // Log.d(TAG, "preview: " + isPreview() + " | visible: " + visible +
                // " | wfs_available: "
                // + waveformManager.areWaveformsAvailable() + " | waveform: "
                // + (waveform == null ? "null" : "yes") + " | renderWaveform: " + renderWaveform
                // + " | animateLogo: " + animateLogo);

                Canvas canvas = null;
                try {
                    canvas = holder.lockCanvas();
                    if (canvas != null) {
                        drawBackground(canvas);
                        if (waveform != null && state.renderWaveform) {
                            drawWaveform(canvas);
                        }
                    }
                } finally {
                    if (canvas != null) {
                        holder.unlockCanvasAndPost(canvas);
                    }
                }

                scheduleFrame();
            }
        };

        private void scheduleFrame() {
            state.skipPendingFrame = false;
            handler.postDelayed(drawFrame, 1000 / FRAME_RATE);
        }

        private void cancelScheduledFrame() {
            state.skipPendingFrame = true;
            handler.removeCallbacks(drawFrame);
        }

        @Override
        public void onFrontSideVisible() {
            this.logoCurrentSide = logoFrontSide;
        }

        @Override
        public void onFlipSideVisible() {
            this.logoCurrentSide = logoFlipSide;
        }

        @Override
        public void onNetworkUp() {
            Log.d(TAG, "Network is back up!");
            if (waveform == null) {
                getNextWaveform();
            }
        }

        @Override
        public void onNetworkDown() {
            Log.e(TAG, "Network is down");
        }
    }
}
