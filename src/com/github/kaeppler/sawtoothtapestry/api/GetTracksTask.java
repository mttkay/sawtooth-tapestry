package com.github.kaeppler.sawtoothtapestry.api;

import java.util.List;

import org.apache.http.HttpResponse;

import com.github.kaeppler.sawtoothtapestry.model.Track;
import com.soundcloud.api.ApiWrapper;

public class GetTracksTask extends SoundCloudApiTask<List<Track>> {

    public GetTracksTask(ApiWrapper api, String resource) {
        super(api, resource);
    }

    @Override
    protected List<Track> handleResponse(HttpResponse response) throws Exception {
        return new TracksParser().parse(response.getEntity().getContent());
    }

}
