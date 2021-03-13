package com.cod3rboy.apnashare.models;

import android.net.Uri;

public class ImageFile extends BasicFile {
    private long mediaStoreId;
    private Uri mediaStoreUri;

    public ImageFile(long mediaStoreId, String imgName, long sizeInBytes, Uri mediaStoreUri) {
        super(imgName, mediaStoreUri.toString(), sizeInBytes);
        this.mediaStoreId = mediaStoreId;
        this.mediaStoreUri = mediaStoreUri;
    }

    public long getMediaStoreId() {
        return mediaStoreId;
    }

    public Uri getMediaStoreUri() {
        return mediaStoreUri;
    }
}
