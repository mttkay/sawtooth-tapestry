package com.github.kaeppler.sawtoothtapestry.waveform;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

import com.github.ignition.support.cache.ImageCache;
import com.github.ignition.support.images.remote.RemoteImageLoaderHandler;
import com.github.ignition.support.images.remote.RemoteImageLoaderJob;
import com.github.kaeppler.sawtoothtapestry.R;
import com.github.kaeppler.sawtoothtapestry.R.id;

public class WaveformDownloader {

    private static final int CACHE_INITIAL_CAPACITY = 20;
    private static final int CACHE_CONCURRENT_THREADS = 1;
    private static final int MAX_RETRIES = 3;
    private static final int DEFAULT_BUFFER_SIZE = 65536;

    private ImageCache imageCache;

    public WaveformDownloader(Context context) {
        imageCache = new ImageCache(CACHE_INITIAL_CAPACITY, Integer.MAX_VALUE,
                CACHE_CONCURRENT_THREADS);
        imageCache.enableDiskCache(context.getApplicationContext(), ImageCache.DISK_CACHE_SDCARD);
    }

    public void downloadWaveform(final Handler handler, String url) {
        RemoteImageLoaderHandler rilHandler = new RemoteImageLoaderHandler(null, url, null) {
            @Override
            protected boolean handleImageLoaded(Bitmap bitmap, Message msg) {
                Message message = handler.obtainMessage(R.id.message_waveform_downloaded, bitmap);
                handler.sendMessage(message);
                return true;
            }
        };
        new Thread(new RemoteImageLoaderJob(url, rilHandler, imageCache, MAX_RETRIES,
                DEFAULT_BUFFER_SIZE)).start();
    }
}
