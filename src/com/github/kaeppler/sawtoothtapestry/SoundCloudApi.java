package com.github.kaeppler.sawtoothtapestry;

import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Env;

public class SoundCloudApi {

    private static final String CLIENT_ID = "847a4b2536a664f81dd58138df6ddee0";
    private static final String CLIENT_SECRET = "c12b9add8acb2fe7d8b0c85d2f361bdf";
    private static final Env ENVIRONMENT = Env.SANDBOX;

    private ApiWrapper api;

    public SoundCloudApi() {
        api = new ApiWrapper(CLIENT_ID, CLIENT_SECRET, null, null, ENVIRONMENT);
        
        //api.login(username, password, scopes)
    }
}
