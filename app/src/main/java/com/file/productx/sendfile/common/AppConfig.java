package com.file.productx.sendfile.common;

import android.app.Application;
import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.lang.ref.WeakReference;

public class AppConfig {

    private static volatile AppConfig instance;

    private static final int DEFAULT_MAX_REQUEST_CONTROL = 5;

    private final WeakReference<Context> context;

    private int maxRequestControl = DEFAULT_MAX_REQUEST_CONTROL;

    private static final String APP_DIR = "ProductXFileTransfer";

    private AppConfig(Context context) {
        this.context = new WeakReference<>(context);
    }

    public void setMaxRequestControl(int maxRequestControl) {
        this.maxRequestControl = maxRequestControl;
    }

    public int getMaxRequestControl(){
        return this.maxRequestControl;
    }

    public static void init(Application application) {
        synchronized (AppConfig.class) {
            if (instance == null) {
                instance = new AppConfig(application);
            }
        }
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            throw new RuntimeException("Call init befor use get instance");
        }
        return instance;
    }

    public String getDownloadDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + APP_DIR + File.separator + "download";
    }

    public String getUploadDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + APP_DIR + File.separator + "upload";
    }

}
