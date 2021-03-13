package com.cod3rboy.apnashare.models;

import android.net.Uri;

public class AudioFile extends BasicFile {
    private long mediaStoreId;
    private Uri mediaStoreUri;

    public AudioFile(long mediaStoreId, String audioName, long sizeInBytes, Uri mediaStoreUri) {
        super(audioName, mediaStoreUri.toString(), sizeInBytes);
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
