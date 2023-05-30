package com.file.productx.sendfile.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TransferEntity {
    @SerializedName("size")
    public long size;

    @SerializedName("name")
    public String fileName;

    @SerializedName("mId")
    public long mId;

    @Expose(serialize = false)
    public int progress;

    @Expose(serialize = false)
    public String localPath;

    @Expose(serialize = false)
    public int type;
}
