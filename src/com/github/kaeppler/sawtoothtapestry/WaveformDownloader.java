package com.github.kaeppler.sawtoothtapestry;

import android.graphics.Bitmap;
import android.os.Message;

import com.github.ignition.support.cache.ImageCache;
import com.github.ignition.support.images.remote.RemoteImageLoaderHandler;
import com.github.ignition.support.images.remote.RemoteImageLoaderJob;

public class WaveformDownloader {

    private static final int CACHE_INITIAL_CAPACITY = 20;
    private static final int CACHE_CONCURRENT_THREADS = 1;
    private static final int MAX_RETRIES = 3;
    private static final int DEFAULT_BUFFER_SIZE = 65536;

    private ImageCache imageCache;

    public WaveformDownloader() {
        imageCache = new ImageCache(CACHE_INITIAL_CAPACITY, Integer.MAX_VALUE,
                CACHE_CONCURRENT_THREADS);
    }

    public void downloadWaveform(final WaveformDownloadCallback callback, String url) {
        RemoteImageLoaderHandler handler = new RemoteImageLoaderHandler(null, url, null) {
            @Override
            protected boolean handleImageLoaded(Bitmap bitmap, Message msg) {
                callback.onWaveformDownloaded(bitmap);
                return true;
            }
        };
        new Thread(new RemoteImageLoaderJob(url, handler, imageCache, MAX_RETRIES,
                DEFAULT_BUFFER_SIZE)).start();
    }

    interface WaveformDownloadCallback {
        void onWaveformDownloaded(Bitmap waveform);
    }
}
