package com.file.productx.sendfile.event;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.text.TextUtils;


public class NetworkStatusReceiver extends BroadcastReceiver {

    private Callback callback;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            int stage = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);

            if (TextUtils.equals(intent.getAction(), "android.net.wifi.WIFI_STATE_CHANGED")) {
                if (this.callback != null) {
                    this.callback.onWifiStageChange(stage);
                }
            }

            if (TextUtils.equals(intent.getAction(), "android.net.wifi.WIFI_AP_STATE_CHANGED")) {
                if (this.callback != null) {
                    this.callback.onHotspotStageChange(stage);
                }
            }
        }
    }

    public interface Callback {

        void onHotspotStageChange(int stage);

        void onWifiStageChange(int stage);
    }

}
