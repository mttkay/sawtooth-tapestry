package com.github.kaeppler.sawtoothtapestry.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectivityChangeBroadcastReceiver extends BroadcastReceiver {

    private NetworkListener listener;

    public ConnectivityChangeBroadcastReceiver(NetworkListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnected()) {
            listener.onNetworkDown();
        } else if (networkInfo.isConnected()) {
            listener.onNetworkUp();
        }
    }

}
