package com.github.kaeppler.sawtoothtapestry.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;

import com.github.kaeppler.sawtoothtapestry.R;
import com.github.kaeppler.sawtoothtapestry.SawtoothWallpaper;
import com.github.kaeppler.sawtoothtapestry.SuperToast;
import com.github.kaeppler.sawtoothtapestry.api.GetTokenTask;
import com.github.kaeppler.sawtoothtapestry.api.SoundCloudApi;
import com.github.kaeppler.sawtoothtapestry.waveform.WaveformUrlManager;
import com.larswerkman.colorpicker.ColorPicker;
import com.soundcloud.api.Token;

//we can't use PreferenceFragment since we support API Level 7 so ignore
//the deprecation warnings
@SuppressWarnings("deprecation")
public class WallpaperSettingsActivity extends PreferenceActivity {

    private static final String SOUNDCLOUD_ACCOUNT = "com.soundcloud.android.account";

    private static final int LOGIN_DIALOG = 0;
    private static final int COLOR_DIALOG = 1;

    private SharedPreferences preferences;
    private Preference connectAccountPref;
    private SoundCloudApi api;

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // android.os.Debug.waitForDebugger();
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);

        // apparently PreferenceFragment has not been backported to the support lib?
        addPreferencesFromResource(R.xml.settings);

        this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.connectAccountPref = findPreference(getString(R.string.settings_key_connect_account));
        this.api = new SoundCloudApi(this);

        setupConnectAccountSetting();
        setupAppearanceSetting();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case LOGIN_DIALOG:
                return createLoginDialog();
            case COLOR_DIALOG:
                return createColorPickerDialog();
            default:
                return super.onCreateDialog(id);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendBroadcast(new Intent(SawtoothWallpaper.ACTION_SETTINGS_CHANGED));
    }

    private Dialog createColorPickerDialog() {
        final ColorPicker colorPicker = new ColorPicker(this);

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.waveformcolor_dialog_title);
        dialog.setView(colorPicker);
        dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                preferences.edit().putInt(getString(R.string.settings_key_waveform_color), colorPicker.getColor()).commit();
                dismissDialog(COLOR_DIALOG);
            }
        });
        dialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissDialog(COLOR_DIALOG);
            }
        });
        return dialog.create();
    }

    private Dialog createLoginDialog() {
        final View content = getLayoutInflater().inflate(R.layout.login_dialog, null);
        final EditText usernameField = (EditText) content.findViewById(android.R.id.text1);

        // try prepopulating the username field
        if (!TextUtils.isEmpty(username)) {
            usernameField.setText(username);
        }

        View loginButton = content.findViewById(R.id.login_dialog_button_ok);
        loginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoginButtonClicked(content, usernameField);
            }
        });
        View cancelButton = content.findViewById(R.id.login_dialog_button_cancel);
        cancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissDialog(LOGIN_DIALOG);
            }
        });

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.login_dialog_title);
        dialog.setView(content);
        return dialog.create();
    }

    private void onLoginButtonClicked(View content, EditText usernameField) {
        EditText passwordField = (EditText) content.findViewById(android.R.id.text2);

        String username = usernameField.getText().toString();
        String password = passwordField.getText().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            SuperToast.error(WallpaperSettingsActivity.this, R.string.login_dialog_error_blank);
        } else {
            this.username = username;

            GetTokenTask task = api.getToken(username, password);
            task.connect(WallpaperSettingsActivity.this);
            task.execute();

            dismissDialog(LOGIN_DIALOG);
        }
    }

    private void logOut() {
        Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    }

    private void setupAppearanceSetting() {
        Preference preference = findPreference(getString(R.string.settings_key_waveform_color));
        preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                showDialog(COLOR_DIALOG);
                return true;
            }
        });
    }

    private void setupConnectAccountSetting() {
        if (api.isLoggedIn()) {
            setupForLoggedInUser();
        } else {
            setupForLoggedOutUser();
        }
    }

    private void setupForLoggedInUser() {
        username = preferences.getString(SettingsKeys.SETTINGS_KEY_USERNAME, "");

        connectAccountPref.setTitle(getString(R.string.settings_disconnect_account, username));
        connectAccountPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                logOut();

                setupForLoggedOutUser();

                return true;
            }
        });

    }

    private void setupForLoggedOutUser() {
        final AccountManager accountManager = AccountManager.get(this);
        final Account[] accounts = accountManager.getAccountsByType(SOUNDCLOUD_ACCOUNT);
        boolean oneClickLoginAvailable = false;

        if (accounts.length > 0) {
            // user is already logged in via SoundCloud app
            oneClickLoginAvailable = true;
            username = accounts[0].name;
        } else {
            // TODO: trigger SC app login
        }

        // TODO: log in via AccountManager doesn't work for some reason; the token returned is
        // always NULL and no error message is issued
        oneClickLoginAvailable = false;

        if (oneClickLoginAvailable) {
            connectAccountPref.setTitle(getString(R.string.settings_connect_as, accounts[0].name));
            connectAccountPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    GetTokenTask task = api.getToken(accountManager, accounts[0]);
                    task.connect(WallpaperSettingsActivity.this);
                    task.execute();
                    return true;
                }
            });
        } else {
            connectAccountPref.setTitle(getString(R.string.settings_connect_your_account));
            connectAccountPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showDialog(LOGIN_DIALOG);
                    return true;
                }
            });

        }
    }

    public void handleTokenReceived(Token token) {
        Editor editor = preferences.edit();
        editor.putString(SettingsKeys.SETTINGS_KEY_TOKEN_ACCESS, token.access);
        editor.putString(SettingsKeys.SETTINGS_KEY_TOKEN_REFRESH, token.refresh);
        editor.putString(SettingsKeys.SETTINGS_KEY_TOKEN_SCOPE, token.scope);
        editor.putString(SettingsKeys.SETTINGS_KEY_USERNAME, username);
        editor.commit();

        setupForLoggedInUser();

        // get the user's favorite tracks' waveforms
        new WaveformUrlManager(this, api, new Handler()).fetchWaveformUrls();

        SuperToast.info(this, R.string.login_success);
    }
}
