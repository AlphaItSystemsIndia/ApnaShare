package com.cod3rboy.apnashare.models;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;

import java.io.File;

public class GeneralFile extends BasicFile {
    private boolean directory;

    // For File Only
    public GeneralFile(File file, Bitmap fileIcon) {
        super(file.getName(), file.getAbsolutePath(), file.length());
        super.setIcon(fileIcon);
        this.directory = false;
    }

    // For Directory Only
    public GeneralFile(File file, Bitmap fileIcon, long childItemsCount) {
        super(file.getName(), file.getAbsolutePath(), childItemsCount);
        super.setIcon(fileIcon);
        this.directory = true;
    }

    // For File with URI path
    public GeneralFile(Uri fileUri, String fileName, Bitmap fileIcon, long sizeInBytes) {
        super(fileName,
                fileUri.toString().startsWith(ContentResolver.SCHEME_CONTENT) ? fileUri.toString() : fileUri.getPath(),
                sizeInBytes);
        super.setIcon(fileIcon);
        this.directory = false;
    }

    public boolean isDirectory() {
        return directory;
    }
}