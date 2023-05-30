package com.file.productx.sendfile;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.file.productx.sendfile.common.Util;
import com.file.productx.sendfile.constant.TransferType;
import com.file.productx.sendfile.databinding.ActivityMainBinding;
import com.file.productx.sendfile.ui.ShareFilesActivity;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = ActivityMainBinding.inflate(this.getLayoutInflater());
        this.setContentView(this.binding.getRoot());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            Util.requestStoreManangerPermission(this);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.R && Util.isNoStoragePermission(this)) {
            ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, Util.REQUEST_STORAGE_PERMISSION_CODE);
        }


        this.initListener();
    }

    private void initListener() {
        this.binding.tvWifi.setOnClickListener(v -> {
            if (!Util.isNetworkConnected(this)) {
                this.showEnableWifiDialog();
                return;
            }

            openWifiShare();
        });

        this.binding.tvHotspot.setOnClickListener(v -> {

        });
    }

    private void openWifiShare() {
        Intent intent = new Intent(this, ShareFilesActivity.class);
        intent.putExtra(Util.EXTRA_TRANSFER_TYPE, TransferType.WIFI_SHARE);
        this.startActivity(intent);
    }

    private void showEnableWifiDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Check wifi")
                .setMessage("Please enable wifi to use this feature!")
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.go_setting, (dialog, which) -> {
                    dialog.dismiss();
                    Util.requestEnableWifi(this);
                })
                .create()
                .show();
    }


}