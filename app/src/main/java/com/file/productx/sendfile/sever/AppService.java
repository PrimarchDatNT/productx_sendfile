package com.file.productx.sendfile.sever;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.file.productx.sendfile.R;
import com.file.productx.sendfile.common.Util;
import com.file.productx.sendfile.event.AppReciver;
import com.file.productx.sendfile.ui.ShareFilesActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AppService extends Service {

    private final IBinder binder = new AppBinder();

    private AppReciver appReciver;

    private AppSever appSever;

    private List<String> listSharePath = new ArrayList<>();

    private int serverPort;

    private NotificationManager mNotifiManager;
    private NotificationCompat.Builder mNotifiBuilder;

    private Callback callback;

    public AppService() {
    }

    public class AppBinder extends Binder {
        public AppService getService() {
            return AppService.this;
        }
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.init();
    }

    private void init() {
        this.initServer();
        this.initReciver();
    }

    private void initServer() {
        this.serverPort = Util.findFreePort();
        this.appSever = new AppSever(this.serverPort, this);
    }

    private void initReciver() {
        this.appReciver = new AppReciver(this);
        IntentFilter intentFilter = Util.configWifiStageFilter();
        intentFilter.addAction(Util.ACTION_NEW_SHARE_LIST);
        intentFilter.addAction(Util.ACTION_START_SERVER);
        intentFilter.addAction(Util.ACTION_STOP_SERVER);
        intentFilter.addAction(Util.ACTION_RECEIVE_NEW_FILE);
        this.registerReceiver(this.appReciver, intentFilter);
    }

    private void onStartServer() {
        NotificationChannel serviceChannel = null;
        String channedId = "WebServiceChannel";
        String channelName = "WebForegroundServiceChannel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            serviceChannel = new NotificationChannel(
                    channedId,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW
            );
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.mNotifiManager = this.getSystemService(NotificationManager.class);
        }

        if (this.mNotifiManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.mNotifiManager.createNotificationChannel(serviceChannel);
            }
        }

        try {
            Intent iContent = new Intent(this, ShareFilesActivity.class);

            this.mNotifiBuilder = new NotificationCompat.Builder(this, channedId);
            this.mNotifiBuilder.setOngoing(true)
                    .setContentTitle(this.getString(R.string.web_notifi_title))
                    .setContentText(this.getString(R.string.web_notifi_msg, Util.getLocalIpAddress(), this.serverPort))
                    .setAutoCancel(true)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, this.getString(R.string.web_notifi_action), this.getServiceIntent(this))
                    .setContentIntent(PendingIntent.getActivity(this, 2059, iContent, 0))
                    .setSmallIcon(android.R.drawable.ic_menu_share);
            this.startForeground(Util.NOTFICATION_ID, this.mNotifiBuilder.build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private PendingIntent getServiceIntent(Context context) {
        Intent intent = new Intent(context, AppService.class);
        intent.setAction(Util.ACTION_STOP_SERVER);
        return PendingIntent.getService(context, 2539, intent, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            this.startSever();

            ArrayList<String> sharePaths = intent.getStringArrayListExtra(Util.EXTRA_SHARE_PATH);
            if (sharePaths != null && !sharePaths.isEmpty()) {
                this.updateShareList(sharePaths);
            }

            this.onStartServer();

            if (TextUtils.equals(intent.getAction(), Util.ACTION_STOP_SERVER)) {
                if (this.mNotifiManager != null) {
                    this.mNotifiManager.cancel(Util.NOTFICATION_ID);
                }

                if (this.callback != null) {
                    this.callback.onStopService();
                }

                this.stopSelf();
            }
        }
        return START_NOT_STICKY;
    }

    public void updateNotification(boolean onStart) {
        if (this.mNotifiBuilder == null) {
            return;
        }

        this.mNotifiBuilder.setContentText(onStart ? this.getString(R.string.web_notifi_msg, Util.getLocalIpAddress(), this.serverPort)
                : this.getString(R.string.interrupt_server));

        if (this.mNotifiManager != null) {
            this.mNotifiManager.notify(Util.NOTFICATION_ID, this.mNotifiBuilder.build());
        } else {
            NotificationManagerCompat.from(this).notify(Util.NOTFICATION_ID, this.mNotifiBuilder.build());
        }
    }

    public void startSever() {
        if (this.appSever == null || this.appSever.isAlive()) {
            return;
        }

        try {
            this.appSever.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        if (this.appSever != null) {
            this.appSever.stop();
        }
    }

    public void updateShareList(ArrayList<String> sharePaths) {
        if (this.appSever == null) {
            return;
        }
        this.appSever.setSharePaths(sharePaths);
    }

    public String getServerAddress() {
        return Util.HTTP_HEADER + Util.getLocalIpAddress() + ":" + this.serverPort;
    }

    @Override
    public void onDestroy() {
        try {
            this.unregisterReceiver(this.appReciver);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.stopServer();
        this.stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    public interface Callback {

        void onReceiveItem(ArrayList<String> uploadPaths);

        void onStopService();
    }
}