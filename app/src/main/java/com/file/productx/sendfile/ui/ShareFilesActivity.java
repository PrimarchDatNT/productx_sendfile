package com.file.productx.sendfile.ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.file.productx.sendfile.R;
import com.file.productx.sendfile.common.Util;
import com.file.productx.sendfile.constant.TransferType;
import com.file.productx.sendfile.databinding.ActivityShareFilesBinding;
import com.file.productx.sendfile.event.NetworkStatusReceiver;
import com.file.productx.sendfile.sever.AppService;
import com.google.zxing.WriterException;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class ShareFilesActivity extends AppCompatActivity implements NetworkStatusReceiver.Callback {

    private static final long CAN_BACK_TIME_DELAY = 10000;

    ActivityShareFilesBinding binding;
    private ServiceConnection connection;
    private AppService appService;
    private boolean isBound;
    private boolean onAppEvent;
    private String sharePw;
    private String hotspotName;
    private TransferType transferType = TransferType.DEFAULT;
    private NetworkStatusReceiver networkReceiver;
    private WifiManager.LocalOnlyHotspotReservation hotspotReservation;
    private boolean canBackOnConnecting;

    private ArrayList<String> sharePaths = new ArrayList<>();
    private ArrayList<String> uploadedPaths = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = ActivityShareFilesBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.binding.getRoot());
        this.init();
    }

    private void init() {
        Intent intent = this.getIntent();
        if (intent == null) {
            this.finish();
            return;
        }

        Serializable extra = intent.getSerializableExtra(Util.EXTRA_TRANSFER_TYPE);
        boolean hasData = extra instanceof TransferType;
        if (!hasData) {
            return;
        }

        this.transferType = (TransferType) extra;

        this.connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                AppService.AppBinder binder = (AppService.AppBinder) service;
                appService = binder.getService();
                isBound = true;
                onConnectedService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isBound = false;
            }
        };

        this.initReceiver();

        this.binding.flError.setOnClickListener(v -> onClickRestartSever());
        this.binding.tvSelect.setOnClickListener(v -> {

        });
    }

    private void initReceiver() {
        this.networkReceiver = new NetworkStatusReceiver();
        this.networkReceiver.setCallback(this);
        try {
            this.registerReceiver(this.networkReceiver, Util.configWifiStageFilter());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onConnectedService() {
        if (this.appService == null) {
            return;
        }

        if (this.transferType == TransferType.WIFI_SHARE || this.transferType == TransferType.HOTSPOT_SHARE) {
            this.displayConnectInformation(this.appService.getServerAddress());
        }
    }

    private void displayConnectInformation(String severAdress) {
        if (TextUtils.isEmpty(severAdress)) {
            this.showErrorLayout(true);
            return;
        }

        this.showErrorLayout(false);
        this.binding.tvServerAddress.setText(severAdress);

        try {
            if (!TextUtils.isEmpty(this.hotspotName) && !TextUtils.isEmpty(this.sharePw)) {
                String text = String.format(this.getString(R.string.share_hotspot_qr), this.hotspotName, this.sharePw);
                Bitmap bitmap = Util.generateQr(Util.calDimenDensity(this, 200), text);

                if (bitmap == null) {
                    return;
                }

                Glide.with(this)
                        .load(bitmap)
                        .into(this.binding.ivServerAddress);
                return;
            }

            Bitmap bitmap = Util.generateQr(Util.calDimenDensity(this, 200), severAdress);
            if (bitmap == null) {
                return;
            }

            Glide.with(this)
                    .load(bitmap)
                    .into(this.binding.ivServerAddress);

        } catch (WriterException e) {
            e.printStackTrace();
            this.binding.ivServerAddress.setVisibility(View.GONE);
        }
    }

    public void showErrorLayout(boolean isError) {
        this.binding.flQrFrame.setVisibility(isError ? View.INVISIBLE : View.VISIBLE);
        this.binding.flStep2.setVisibility(isError ? View.INVISIBLE : View.VISIBLE);
        this.binding.loadingView.setVisibility(isError ? View.VISIBLE : View.GONE);
    }

    private void disconnectService() {
        if (this.isBound && this.connection != null) {
            try {
                this.unbindService(this.connection);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (this.appService != null) {
                this.appService.stopSelf();
            }
        }

        if (this.transferType == TransferType.HOTSPOT_SHARE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.turnOffHotspot();
            } else {
                this.disableConfigured();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void turnOffHotspot() {
        if (this.hotspotReservation != null) {
            this.hotspotReservation.close();
            this.hotspotReservation = null;
        }

        WifiManager manager = Util.getWifiManager(this);

        Method[] methods = WifiManager.class.getDeclaredMethods();
        if (methods == null) {
            return;
        }

        for (Method method : methods) {
            if (TextUtils.equals(method.getName(), "cancelLocalOnlyHotspotRequest")) {
                try {
                    method.invoke(manager);
                    break;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

            if (TextUtils.equals(method.getName(), "stopLocalOnlyHotspot")) {
                try {
                    method.invoke(manager);
                    break;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void disableConfigured() {
        WifiManager wifiManager = Util.getWifiManager(this);
        if (wifiManager == null) {
            return;
        }

        Method[] methods = WifiManager.class.getDeclaredMethods();
        if (methods == null) {
            return;
        }

        Method setHotspotEnable = null;

        for (Method method : methods) {
            if (TextUtils.equals(method.getName(), "setWifiApEnabled")) {
                setHotspotEnable = method;
                break;
            }
        }

        WifiConfiguration configuration = Util.getNetworkConfig(wifiManager);

        if (setHotspotEnable == null || configuration == null) {
            return;
        }

        try {
            setHotspotEnable.invoke(wifiManager, configuration, false);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void unRegisterReceiver() {
        if (this.networkReceiver != null) {
            try {
                this.unregisterReceiver(this.networkReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updateWifiShare() {
        if (this.appService == null) {
            this.startWebSerivce();
        } else {
            this.appService.startSever();
            this.appService.updateNotification(true);
        }


        this.binding.flError.setVisibility(View.GONE);
    }

    private void startWebSerivce() {
        Intent iService = new Intent(this, AppService.class);
        iService.putStringArrayListExtra(Util.EXTRA_SHARE_PATH, this.sharePaths);
        this.startService(iService);
    }

    public void onClickRestartSever() {
        if (this.transferType == TransferType.WIFI_SHARE) {
            if (Util.isNetworkConnected(this)) {
                this.updateWifiShare();
            } else {
                Util.requestEnableWifi(this);
            }
        }

        if (this.transferType == TransferType.HOTSPOT_SHARE) {
            this.onShareHotspot();
        }
    }

    public void onShareHotspot() {
        if (!Util.isLocationEnabled(this)) {
            Toast.makeText(this, R.string.enable_location_service_msg, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Util.isAvailableWriteSettingPermission(this)) {
            this.showPermissionDialog(Util.REQUEST_WRITE_SETTING_PMS_CODE);
            return;
        }

        if (!Util.isAvailableHotspotPermission(this)) {
            Util.requestHotspotPermission(this);
            return;
        }

        this.turnOnHotspot();
    }

    @SuppressLint("MissingPermission")
    private void turnOnHotspot() {
        WifiManager manager = Util.getWifiManager(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.binding.loadingView.setVisibility(View.VISIBLE);


            if (Util.isEnableHotspot(this)) {
                this.turnOffHotspot();
            }

            this.canBackOnConnecting = false;
            this.binding.getRoot().postDelayed(() -> this.canBackOnConnecting = true, CAN_BACK_TIME_DELAY);

            try {
                manager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
                    @Override
                    public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                        super.onStarted(reservation);
                        binding.loadingView.setVisibility(View.GONE);
                        binding.flError.setVisibility(View.GONE);
                        hotspotReservation = reservation;
                        WifiConfiguration wifiConfiguration = hotspotReservation.getWifiConfiguration();
                        onActivedHotot(wifiConfiguration.SSID, wifiConfiguration.preSharedKey);
                    }

                    @Override
                    public void onStopped() {
                        super.onStopped();
                        binding.loadingView.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailed(int reason) {
                        super.onFailed(reason);
                        binding.loadingView.setVisibility(View.GONE);
                    }

                }, new Handler(Looper.getMainLooper()));

            } catch (Exception e) {
                Toast.makeText(this, R.string.enable_location_service_msg, Toast.LENGTH_SHORT).show();
                binding.loadingView.setVisibility(View.GONE);
                e.printStackTrace();
            }
            return;
        }

        if (Util.isEnableHotspot(this)) {
            this.disableConfigured();
        }

        this.enableConfigured(manager);
    }

    private void enableConfigured(@NonNull WifiManager wifiManager) {
        this.binding.loadingView.setVisibility(View.VISIBLE);

        wifiManager.setWifiEnabled(false);
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = Build.MODEL;
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        for (Method method : WifiManager.class.getDeclaredMethods()) {
            if (TextUtils.equals(method.getName(), "setWifiApEnabled")) {
                try {
                    method.invoke(wifiManager, wifiConfiguration, true);
                    this.onAppEvent = true;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void onActivedHotot(String ssid, String password) {
        if (TextUtils.isEmpty(ssid)) {
            Toast.makeText(this, R.string.err_start_hotspot, Toast.LENGTH_SHORT).show();
            return;
        }

        this.hotspotName = ssid;
        this.sharePw = password;

        this.transferType = TransferType.HOTSPOT_SHARE;
        this.bindService(new Intent(this, AppService.class), this.connection, Context.BIND_AUTO_CREATE);
        this.startWebSerivce();
    }

    private void showPermissionDialog(int reqCode) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_permission)
                .setMessage(reqCode == Util.REQUEST_WRITE_SETTING_PMS_CODE ? R.string.write_setting_pms_msg : R.string.hotspot_pms_msg)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.go_setting, (dialog, which) -> {
                    dialog.dismiss();
                    goSetting(reqCode);
                })
                .create()
                .show();
    }

    private void goSetting(int requestCode) {
        final Intent intent = new Intent();
        if (requestCode == Util.REQUEST_LOCAL_PMS_CODE) {
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.setData(Uri.parse("package:" + this.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        }

        if (requestCode == Util.REQUEST_WRITE_SETTING_PMS_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                intent.setAction(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            }
            intent.setData(Uri.parse("package:" + this.getPackageName()));
        }
        this.startActivityForResult(intent, requestCode);
    }

    private void showExitDialog() {
        String message = this.getString(R.string.exit_feature_msg);

        if (this.transferType == TransferType.WIFI_SHARE) {
            message = this.getString(R.string.exit_wifi_feature_msg);
        }

        if (this.transferType == TransferType.HOTSPOT_SHARE) {
            message = this.getString(R.string.exit_hotspot_feature_msg);
        }

        if (this.transferType == TransferType.RECEIVE) {
            message = this.getString(R.string.exit_recieve_feaute);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.exit_feature)
                .setMessage(message)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.btn_ok, (dialog, which) -> {
                    dialog.dismiss();
                    disconnectService();
                    super.onBackPressed();
                })
                .create()
                .show();
    }

    @Override
    public void onBackPressed() {
        if (this.transferType != TransferType.DEFAULT) {
            this.showExitDialog();
            return;
        }
        if (this.binding.loadingView.getVisibility() == View.GONE || this.canBackOnConnecting) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        this.unRegisterReceiver();
        this.disconnectService();
        super.onDestroy();
    }

    @Override
    public void onHotspotStageChange(int stage) {
        if (stage == Util.WIFI_AP_STATE_DISABLED) {
            if (this.transferType != TransferType.HOTSPOT_SHARE) {
                return;
            }

            if (this.appService != null) {
                this.appService.stopServer();
                this.appService.updateNotification(false);
            }

            this.binding.flError.setVisibility(View.VISIBLE);
        }

        if (stage == Util.WIFI_AP_STATE_FAILED) {
            this.binding.loadingView.setVisibility(View.GONE);
        }

        if (stage == Util.WIFI_AP_STATE_ENABLED) {
            if (this.onAppEvent) {
                WifiManager wifiManager = Util.getWifiManager(this);
                WifiConfiguration configuration = Util.getNetworkConfig(wifiManager);
                if (configuration == null) {
                    return;
                }

                if (this.transferType == TransferType.DEFAULT) {
                    this.onActivedHotot(configuration.SSID, configuration.preSharedKey);
                    this.onAppEvent = false;
                }

                if (this.transferType == TransferType.HOTSPOT_SHARE) {
                    if (this.appService == null) {
                        this.startWebSerivce();
                    } else {
                        this.appService.startSever();
                        this.appService.updateNotification(true);
                    }


                    this.binding.flError.setVisibility(View.GONE);
                }

                this.binding.loadingView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onWifiStageChange(int stage) {
        if (stage == Util.WIFI_STATE_DISABLED || stage == Util.WIFI_STATE_UNKNOWN) {
            if (this.transferType != TransferType.WIFI_SHARE) {
                return;
            }

            if (this.appService != null) {
                this.appService.stopServer();
                this.appService.updateNotification(false);
            }

            this.binding.flError.setVisibility(View.VISIBLE);
        }
    }
}