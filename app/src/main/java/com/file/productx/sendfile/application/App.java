package com.file.productx.sendfile.application;

import android.app.Application;

import com.file.productx.sendfile.common.AppConfig;

public class App extends Application {

   @Override
   public void onCreate() {
      super.onCreate();
      AppConfig.init(this);
   }
}
