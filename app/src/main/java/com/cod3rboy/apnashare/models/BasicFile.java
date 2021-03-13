package com.cod3rboy.apnashare.models;

import android.graphics.Bitmap;

public abstract class BasicFile {
    private String fileName;
    private Bitmap fileIcon;
    private String path;
    private long bytesOrItemsCount;

    protected BasicFile(String fileName, String path, long bytesOrItemsCount) {
        this.fileName = fileName;
        this.fileIcon = null;
        this.path = path;
        this.bytesOrItemsCount = bytesOrItemsCount;
    }

    public void setIcon(Bitmap icon) {
        this.fileIcon = icon;
    }

    public final String getFileName() {
        return fileName;
    }

    public final Bitmap getIcon() {
        return fileIcon;
    }

    public final String getUid() {
        return path;
    }

    public final String getPath() {
        return path;
    }

    public final long getBytesOrItemsCount() {
        return bytesOrItemsCount;
    }
}
