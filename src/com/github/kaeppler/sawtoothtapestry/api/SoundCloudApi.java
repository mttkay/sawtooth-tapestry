package com.github.kaeppler.sawtoothtapestry.api;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.github.kaeppler.sawtoothtapestry.settings.SettingsKeys;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Env;
import com.soundcloud.api.Token;

public class SoundCloudApi {

    private static final String TAG = SoundCloudApi.class.getSimpleName();
    private static final String CLIENT_ID = "847a4b2536a664f81dd58138df6ddee0";
    private static final String CLIENT_SECRET = "c12b9add8acb2fe7d8b0c85d2f361bdf";
    private static final Env ENVIRONMENT = Env.LIVE;

    private ApiWrapper api;

    private SharedPreferences preferences;

    public SoundCloudApi(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // check if we already have a token and read it
        Token token = restoreToken(context);

        api = new ApiWrapper(CLIENT_ID, CLIENT_SECRET, null, token, ENVIRONMENT);
    }

    private Token restoreToken(Context context) {
        Token token = null;
        if (isLoggedIn()) {
            String tokenAccess = preferences
                    .getString(SettingsKeys.SETTINGS_KEY_TOKEN_ACCESS, null);
            String tokenRefresh = preferences.getString(SettingsKeys.SETTINGS_KEY_TOKEN_REFRESH,
                    null);
            String tokenScope = preferences.getString(SettingsKeys.SETTINGS_KEY_TOKEN_SCOPE, null);
            token = new Token(tokenAccess, tokenRefresh, tokenScope);
            Log.d(TAG, "Found access token: " + token);
        }
        return token;
    }

    public boolean isLoggedIn() {
        return preferences.contains(SettingsKeys.SETTINGS_KEY_TOKEN_ACCESS);
    }

    public GetTokenTask getToken(AccountManager accountManager, Account account) {
        return new GetTokenTask(api, accountManager, account);
    }

    public GetTokenTask getToken(String username, String password) {
        return new GetTokenTask(api, username, password);
    }
}
