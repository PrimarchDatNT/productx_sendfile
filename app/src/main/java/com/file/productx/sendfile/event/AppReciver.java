package com.file.productx.sendfile.event;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.file.productx.sendfile.common.Util;
import com.file.productx.sendfile.sever.AppService;

import java.util.ArrayList;

public class AppReciver extends BroadcastReceiver {

    private final AppService appService;

    public AppReciver(AppService appService) {
        this.appService = appService;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        if (TextUtils.equals(intent.getAction(), Util.ACTION_START_SERVER)) {
            this.appService.startSever();
        }

        if (TextUtils.equals(intent.getAction(), Util.ACTION_STOP_SERVER)) {
            this.appService.stopSelf();
        }

        if (TextUtils.equals(intent.getAction(), Util.ACTION_NEW_SHARE_LIST)) {
            ArrayList<String> sharePaths = intent.getStringArrayListExtra(Util.EXTRA_SHARE_PATH);
            if (sharePaths == null || sharePaths.isEmpty()) {
                return;
            }
            this.appService.updateShareList(sharePaths);
        }

        if (TextUtils.equals(intent.getAction(), Util.ACTION_RECEIVE_NEW_FILE)) {

            String filePath = intent.getStringExtra(Util.EXTRA_RECEIVE_FILE_PATH);
            if (filePath == null || TextUtils.isEmpty(filePath)) {
                return;
            }

     /*       this.appService.uploadedPaths.add(filePath);
            this.appService.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(filePath))));
            EventBus.getDefault().post(new UpdateFileEvent(UpdateEventTag.TAG_UPDATE_LOCAL_DATA));
            if (this.appService.callback != null) {
                this.appService.callback.onReceiveItem(this.appService.uploadedPaths);
            }*/
        }

    }
}
