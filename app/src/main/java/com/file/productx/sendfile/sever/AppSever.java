package com.file.productx.sendfile.sever;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.file.productx.sendfile.R;
import com.file.productx.sendfile.common.AppConfig;
import com.file.productx.sendfile.common.Util;
import com.file.productx.sendfile.model.TransferEntity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import fi.iki.elonen.NanoHTTPD;

class AppSever extends NanoHTTPD {

    private static final String REQ_UPLOAD = "_req_upload";
    private static final String REQ_DOWNLOAD = "_req_download";

    private static final LinkedList<String> REQUEST_POOL = new LinkedList<>();

    private final WeakReference<Context> appContext;
    private final WeakReference<AssetManager> appAssetManager;

    private final List<String> sharePaths = new ArrayList<>();
    private List<TransferEntity> listEntity = new ArrayList<>();

    public AppSever(int port, Context appContext) {
        super(port);
        this.appContext = new WeakReference<>(appContext);
        this.appAssetManager = new WeakReference<>(appContext.getAssets());
    }

    public void setSharePaths(List<String> sharePaths) {
        this.sharePaths.clear();
        this.sharePaths.addAll(sharePaths);
        this.listEntity = this.getTransferObjects(this.sharePaths);
    }

    private synchronized void pollRequest() {
        REQUEST_POOL.poll();
    }

    private synchronized void managerRequest(String reqProperty) {
        REQUEST_POOL.add(reqProperty);
    }

    @NonNull
    private String renderWebPage() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            InputStream inputStream = this.openWebAssetFile("home.html");
            int len;

            while ((len = inputStream.read()) != -1) {
                stream.write(len);
                stream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stream.toString();
    }


    @NonNull
    private byte[] readWebAssetFile(String pageName) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        InputStream inputStream = this.openWebAssetFile(pageName);
        int len;

        while ((len = inputStream.read()) != -1) {
            stream.write(len);
            stream.flush();
        }

        return stream.toByteArray();
    }

    private InputStream openWebAssetFile(String fileName) throws IOException {
        return this.appAssetManager.get().open("webshare" + File.separator + fileName);
    }

    @Override
    public Response serve(@NonNull IHTTPSession session) {

        String uri = session.getUri();
        Log.d(AppSever.class.getSimpleName(), "serve: " + uri);

        if (TextUtils.equals(uri, "/")) {
            return newFixedLengthResponse(this.renderWebPage());
        }

        if (uri.startsWith("/content_download")) {
            return this.getListContentResponse();
        }

        if (uri.startsWith("/upload")) {
            if (REQUEST_POOL.size() >= AppConfig.getInstance().getMaxRequestControl()) {
                return this.getMessageResponse(Response.Status.OK, this.appContext.get().getString(R.string.sever_limited_msg));
            } else {
                try {
                    return this.uploadResponse(session);
                } catch (IOException e) {
                    e.printStackTrace();
                    this.pollRequest();
                    return this.getMessageResponse(Response.Status.NO_CONTENT, Util.UPLOAD_FAIL_MSG);
                }
            }
        }

        if (uri.startsWith("/ping")) {
            try {
                long index = Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1));
                return this.getDownloadAvailableItem(index);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return this.getMessageResponse(Response.Status.OK, Util.UNABLE_DOWNLOAD);
            }
        }

        if (uri.startsWith("/download")) {
            if (REQUEST_POOL.size() >= 5) {
                return this.getMessageResponse(Response.Status.OK, this.appContext.get().getString(R.string.sever_limited_msg));
            } else {
                try {
                    return this.getDownloadResponse(Long.parseLong(uri.substring(uri.lastIndexOf("/") + 1)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return this.getMessageResponse(Response.Status.NO_CONTENT, Util.UNABLE_DOWNLOAD);
                }
            }
        }

        return this.getAssetsResponse(uri);
    }


    private Response getMessageResponse(Response.Status status, String s) {
        return newFixedLengthResponse(status, NanoHTTPD.MIME_HTML, s);
    }


    @NonNull
    private Response getListContentResponse() {
        Response response = newFixedLengthResponse(Response.Status.OK, "application/json", this.getTransferJson());
        response.addHeader("Access-Control-Allow-Origin", "* ");
        return response;
    }

    @NonNull
    private List<TransferEntity> getTransferObjects(List<String> listPath) {
        if (listPath == null) {
            return new ArrayList<>();
        }

        if (listPath.isEmpty()) {
            return new ArrayList<>();
        }

        List<TransferEntity> entities = new ArrayList<>();

        for (String path : this.sharePaths) {
            File file = new File(path);
            if (!file.exists()) {
                continue;
            }
            TransferEntity model = new TransferEntity();
            model.fileName = file.getName();
            model.size = file.length();
            model.localPath = file.getAbsolutePath();
            model.mId = System.nanoTime();
            entities.add(model);
        }

        return entities;
    }

    private String getTransferJson() {
        if (this.listEntity == null) {
            return "[]";
        }

        if (this.listEntity.isEmpty()) {
            return "[]";
        }

        return new Gson().toJson(new ArrayList<>(this.listEntity), new TypeToken<List<TransferEntity>>() {
        }.getType());
    }

    @Override
    public void stop() {
        REQUEST_POOL.clear();
        super.stop();
    }

    private String getSendPath(long downloadId) {
        if (this.listEntity == null || this.listEntity.isEmpty()) {
            return "";
        }
        for (TransferEntity model : this.listEntity) {
            if (model.mId == downloadId) {
                return model.localPath;
            }
        }

        return "";
    }

    @NonNull
    private Response getDownloadAvailableItem(long index) {
        String sendPath = this.getSendPath(index);
        File file = new File(sendPath);
        return file.exists() ? this.getMessageResponse(Response.Status.OK, Util.AVAILABLE_DOWNLOAD)
                : this.getMessageResponse(Response.Status.OK, Util.UNABLE_DOWNLOAD);
    }


    @NonNull
    private Response getDownloadResponse(long index) {
        String sendPath = this.getSendPath(index);
        File file = new File(sendPath);
        if (!file.exists()) {
            return this.getMessageResponse(Response.Status.NO_CONTENT, Util.UNABLE_DOWNLOAD);
        }

        Response response;
        try {
            FileInputStream data = new ReqInputStream(file);
            response = newFixedLengthResponse(Response.Status.OK, "file/*", data, file.length());
            response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            return response;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return this.getMessageResponse(Response.Status.NO_CONTENT, Util.UNABLE_DOWNLOAD);
    }

    private Response getAssetsResponse(@NonNull String uri) {
        try {
            byte[] byteData = this.readWebAssetFile(uri.substring(1));
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteData);
            return newFixedLengthResponse(Response.Status.OK, uri.contains(".svg") ? "image/svg+xml" : "file/*", byteArrayInputStream, byteData.length);
        } catch (IOException e) {
            e.printStackTrace();
            return this.getMessageResponse(Response.Status.NO_CONTENT, Util.UNABLE_DOWNLOAD);
        }
    }

    private Response uploadResponse(@NonNull IHTTPSession session) throws IOException {

        String reqProperty = session.getHeaders().get("http-client-ip") + REQ_UPLOAD;
        this.managerRequest(reqProperty);

        long size = Long.parseLong(session.getHeaders().get("content-length"));
        String orgName = session.getHeaders().get("file_name");
        if (TextUtils.isEmpty(orgName)) {
            this.pollRequest();
            return this.getMessageResponse(Response.Status.NO_CONTENT, Util.UPLOAD_FAIL_MSG);
        }

        String uniqueFileName = Util.getUniqueFileName(Util.getUploadDocumentFile(), orgName, true);
        File file = new File(AppConfig.getInstance().getUploadDir() + File.separator + uniqueFileName);

        OutputStream outputStream = new FileOutputStream(file);
        int rlen = 0;

        byte[] buf = new byte[1024];
        while (rlen >= 0 && size > 0) {
            rlen = session.getInputStream().read(buf, 0, (int) Math.min(size, 1024));
            size -= rlen;
            if (rlen > 0) {
                outputStream.write(buf, 0, rlen);
            }
        }

        outputStream.flush();
        outputStream.close();

        Intent intent = new Intent(Util.ACTION_RECEIVE_NEW_FILE);
        intent.putExtra(Util.EXTRA_RECEIVE_FILE_PATH, file.getPath());
        this.appContext.get().sendBroadcast(intent);
        this.pollRequest();
        return newFixedLengthResponse(this.appContext.get().getString(R.string.upload_success_msg));
    }

    private class ReqInputStream extends FileInputStream {

        public ReqInputStream(File file) throws FileNotFoundException {
            super(file);
            managerRequest(System.currentTimeMillis() + REQ_DOWNLOAD);
        }

        @Override
        public void close() throws IOException {
            super.close();
            pollRequest();
        }
    }

}