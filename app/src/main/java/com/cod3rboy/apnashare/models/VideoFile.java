package com.cod3rboy.apnashare.models;

import android.net.Uri;

public class VideoFile extends BasicFile {
    private long mediaStoreId;
    private Uri mediaStoreUri;
    private long duration;

    public VideoFile(long mediaStoreId, String videoName, long sizeInBytes, Uri mediaStoreUri) {
        super(videoName, mediaStoreUri.toString(), sizeInBytes);
        this.mediaStoreId = mediaStoreId;
        this.mediaStoreUri = mediaStoreUri;
        this.duration = -1;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getMediaStoreId() {
        return mediaStoreId;
    }

    public Uri getMediaStoreUri() {
        return mediaStoreUri;
    }

    public long getDuration() {
        return duration;
    }
}