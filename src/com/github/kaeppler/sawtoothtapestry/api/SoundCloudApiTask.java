package com.github.kaeppler.sawtoothtapestry.api;

import org.apache.http.HttpResponse;

import android.content.Context;
import android.util.Log;

import com.github.ignition.core.tasks.IgnitedAsyncTask;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Request;

public abstract class SoundCloudApiTask<ModelT> extends
        IgnitedAsyncTask<Context, Object, Void, ModelT> {

    private static final String TAG = SoundCloudApiTask.class.getSimpleName();

    private ApiWrapper api;
    private String resource;

    public SoundCloudApiTask(ApiWrapper api, String resource) {
        this.api = api;
        this.resource = resource;
    }

    @Override
    public ModelT run(Object... params) throws Exception {
        Request request = Request.to(resource, params);
        Log.d(TAG, "Sending request to " + request.toUrl());
        HttpResponse response = api.get(request);
        return handleResponse(response);
    }

    protected abstract ModelT handleResponse(HttpResponse response) throws Exception;
}
