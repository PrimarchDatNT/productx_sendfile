package com.file.productx.sendfile.common;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.documentfile.provider.DocumentFile;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Hashtable;

public class Util {

    public static final String EXTRA_SELECT_ONLY_FILE = "extra_select_only_file";
    public static final String EXTRA_SHARE_PATH = "extra_share_path";
    public static final String EXTRA_TRANSFER_TYPE = "extra_transfer_type";
    public static final String EXTRA_SCAN_RESULT = "extra_scan_result";
    public static final String EXTRA_RECEIVE_FILE_PATH = "extra_receive_file_path";
    public static final String AVAILABLE_DOWNLOAD = "Available";
    public static final String UNABLE_DOWNLOAD = "No content";
    public static final String UPLOAD_FAIL_MSG = "Up load fail!";

    public static final String ACTION_NEW_SHARE_LIST = "action_new_share_list";
    public static final String ACTION_START_SERVER = "action_start_server";
    public static final String ACTION_STOP_SERVER = "action_stop_server";
    public static final String ACTION_RECEIVE_NEW_FILE = "action_receive_new_file";

    public static final int MIN_COUNT = 1;
    public static final int MAX_COUNT = 9999;

    public static final int REQUEST_FILE_MANAGER_ACCESS_CODE = 1593;
    public static final int REQUEST_STORAGE_PERMISSION_CODE = 1489;

    public static final int REQUEST_SENABLE_WIFI = 139;

    public static final String HTTP_HEADER = "http://";

    public static final int DEFAULT_WEB_SERVER_PORT = 1001;
    public static final int MAX_PORT_NUMBER = 49151;

    public static final int NOTFICATION_ID = 1820;

    public static final int WIFI_STATE_DISABLED = 1;
    public static final int WIFI_STATE_UNKNOWN = 4;

    public static final int WIFI_AP_STATE_DISABLED = 11;
    /* public static final int WIFI_AP_STATE_ENABLING = 12;*/
    public static final int WIFI_AP_STATE_ENABLED = 13;
    public static final int WIFI_AP_STATE_FAILED = 14;

    public static final int REQ_SELECT_FILE_SHARE_CODE = 135;
    public static final int REQ_SELECT_UPLOAD_FILE = 136;
    public static final int REQ_SCAN_QR = 137;
    /*public static final int REQ_ACCESS_STORAGE_FRAMEWORK = 138;*/
    public static final int REQUEST_ENABLE_WIFI = 139;
    public static final int REQUEST_WRITE_SETTING_PMS_CODE = 4096;
    public static final int REQUEST_LOCAL_PMS_CODE = 4095;


    public static void requestEnableWifi(@NonNull Activity activity) {
        activity.startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), Util.REQUEST_SENABLE_WIFI);
    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode;

        try {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    }

    public static boolean isAvailableWriteSettingPermission(Context context) {
        boolean retVal = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            retVal = Settings.System.canWrite(context);
        }
        return retVal;
    }

    public static boolean isAvailableHotspotPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static void requestHotspotPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, REQUEST_LOCAL_PMS_CODE);
    }

    public static boolean isEnableHotspot(Context context) {
        WifiManager manager = Util.getWifiManager(context);
        if (manager == null) {
            return false;
        }

        Method[] methods = WifiManager.class.getDeclaredMethods();
        if (methods == null) {
            return false;
        }

        for (Method method : methods) {
            if (TextUtils.equals(method.getName(), "isWifiApEnabled")) {
                try {
                    Boolean isEnable = (Boolean) method.invoke(manager);
                    return isEnable != null && isEnable;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }


    public static int findFreePort() {
        for (int i = DEFAULT_WEB_SERVER_PORT; i <= MAX_PORT_NUMBER; i++) {
            if (isAvailablePort(i)) {
                return i;
            }
        }
        throw new RuntimeException("Could not find an isAvailable port between " + DEFAULT_WEB_SERVER_PORT + " and " + MAX_PORT_NUMBER);
    }

    private static boolean isAvailablePort(final int port) {
        ServerSocket serverSocket = null;
        DatagramSocket dataSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            dataSocket = new DatagramSocket(port);
            dataSocket.setReuseAddress(true);
            return true;
        } catch (final IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (dataSocket != null) {
                dataSocket.close();
            }
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean isNetworkConnected(@NonNull Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = null;
        if (cm != null) {
            info = cm.getActiveNetworkInfo();
        }
        return info != null && info.isConnected();
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public static void requestStoreManangerPermission(@NonNull Activity activity) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivityForResult(intent, Util.REQUEST_FILE_MANAGER_ACCESS_CODE);
    }

    public static boolean isNoStoragePermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    @NonNull
    public static IntentFilter configWifiStageFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        intentFilter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        return intentFilter;
    }

    public static String getUniqueFileName(DocumentFile documentFolder, String fileName, boolean tryActualFile) {
        if (tryActualFile && documentFolder.findFile(fileName) == null) {
            return fileName;
        }

        int pathStartPosition = fileName.lastIndexOf(".");

        String mergedName = pathStartPosition != -1 ? fileName.substring(0, pathStartPosition) : fileName;
        String fileExtension = pathStartPosition != -1 ? fileName.substring(pathStartPosition) : "";

        if (mergedName.length() == 0 && fileExtension.length() > 0) {
            mergedName = fileExtension;
            fileExtension = "";
        }

        for (int exceed = MIN_COUNT; exceed < MAX_COUNT; exceed++) {
            String newName = mergedName + " (" + exceed + ")" + fileExtension;

            if (documentFolder.findFile(newName) == null) {
                return newName;
            }
        }

        return fileName;
    }

    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return "";
    }

    @NonNull
    public static DocumentFile getUploadDocumentFile() {
        String defaultPath = AppConfig.getInstance().getUploadDir();
        File defaultFolder = new File(defaultPath);

        if (!defaultFolder.exists()) {
            System.out.println(defaultFolder.mkdirs());
        }

        return DocumentFile.fromFile(defaultFolder);
    }

    @NonNull
    public static DocumentFile getDownloadDocumentFile() {
        String defaultPath = AppConfig.getInstance().getDownloadDir();
        File defaultFolder = new File(defaultPath);

        if (!defaultFolder.exists()) {
            System.out.println(defaultFolder.mkdirs());
        }

        return DocumentFile.fromFile(defaultFolder);
    }

    public static WifiManager getWifiManager(@NonNull Context context) {
        return (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public static WifiConfiguration getNetworkConfig(WifiManager wifiManager) {
        if (wifiManager == null) {
            return null;
        }
        Method[] methods = WifiManager.class.getDeclaredMethods();
        for (Method method : methods) {
            if (TextUtils.equals(method.getName(), "getWifiApConfiguration")) {
                try {
                    return (WifiConfiguration) method.invoke(wifiManager);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static int calDimenDensity(@NonNull Context context, float size) {
        return (int) (context.getResources().getDisplayMetrics().density * size);
    }

    public static Bitmap generateQr(int size, String inputText) throws WriterException {
        Hashtable<EncodeHintType, String> hints = new Hashtable<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();

        BitMatrix bitMatrix = multiFormatWriter.encode(inputText, BarcodeFormat.QR_CODE, size, size, hints);
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();

        return barcodeEncoder.createBitmap(bitMatrix);
    }
}
