package com.github.kaeppler.sawtoothtapestry.api;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.util.Log;

import com.github.ignition.core.exceptions.ResourceMessageException;
import com.github.ignition.core.tasks.IgnitedAsyncTask;
import com.github.kaeppler.sawtoothtapestry.SuperToast;
import com.github.kaeppler.sawtoothtapestry.settings.WallpaperSettingsActivity;
import com.soundcloud.api.ApiWrapper;
import com.soundcloud.api.Token;

public class GetTokenTask extends IgnitedAsyncTask<WallpaperSettingsActivity, Void, Void, Token> {

    private static final String TAG = GetTokenTask.class.getSimpleName();

    private ApiWrapper api;
    private Account account;
    private AccountManager accountManager;
    private String username, password;

    public GetTokenTask(ApiWrapper api, AccountManager accountManager, Account account) {
        this.api = api;
        this.accountManager = accountManager;
        this.account = account;
        this.username = account.name;
    }

    public GetTokenTask(ApiWrapper api, String username, String password) {
        this.api = api;
        this.username = username;
        this.password = password;
    }

    @Override
    public boolean onTaskStarted(WallpaperSettingsActivity context) {
        context.setProgressBarIndeterminateVisibility(true);
        return true;
    }

    @Override
    public Token run(Void... params) throws Exception {
        Token token = null;
        try {
            String tokenScope = Token.SCOPE_NON_EXPIRING;
            if (account == null) {
                token = api.login(username, password, tokenScope);
            } else {
                String tokenString = accountManager.blockingGetAuthToken(account, "access_token",
                        true);
                if (tokenString == null) {
                    throw new LoginFailedException();
                }
                token = new Token(tokenString, null, tokenScope);
            }
        } catch (IOException e) {
            throw new LoginFailedException();
        }

        return token;
    }

    @Override
    public boolean onTaskCompleted(WallpaperSettingsActivity context, Token result) {
        context.setProgressBarIndeterminateVisibility(false);
        return true;
    }

    @Override
    public boolean onTaskSuccess(WallpaperSettingsActivity context, Token token) {
        Log.d(TAG, "Got access token: " + token);
        context.handleTokenReceived(token);
        return true;
    }

    @Override
    public boolean onTaskFailed(WallpaperSettingsActivity context, Exception error) {
        super.onTaskFailed(context, error);

        String message = null;
        if (error instanceof ResourceMessageException) {
            int resId = ((ResourceMessageException) error).getClientMessageResourceId();
            message = context.getString(resId);
        } else {
            message = error.getLocalizedMessage();
        }

        SuperToast.error(context, message);

        return true;
    }
}
