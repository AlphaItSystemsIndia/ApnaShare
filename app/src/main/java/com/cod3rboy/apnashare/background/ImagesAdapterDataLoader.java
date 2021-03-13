package com.cod3rboy.apnashare.background;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.content.AsyncTaskLoader;

import java.util.ArrayList;
import java.util.Locale;

import com.cod3rboy.apnashare.models.ImageFile;

public class ImagesAdapterDataLoader extends AsyncTaskLoader<ArrayList<ImageFile>> {

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    public ImagesAdapterDataLoader(@NonNull Context context) {
        super(context);
    }


    @Nullable
    @Override
    public ArrayList<ImageFile> loadInBackground() {
        // Load Images data from media store
        ArrayList<ImageFile> imagesData = new ArrayList<>();
        ContentResolver imagesResolver = getContext().getContentResolver();
        Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String querySuffixSort = String.format(Locale.getDefault(), "%s DESC", MediaStore.Images.Media._ID); // Add sorting column
        Cursor imagesCursor = imagesResolver.query(
                imagesUri,
                new String[]{MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.SIZE},
                null,
                null,
                querySuffixSort
        );
        if (imagesCursor != null && imagesCursor.moveToFirst()) {
            int colId = imagesCursor.getColumnIndex(MediaStore.Images.Media._ID);
            int colName = imagesCursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
            int colSize = imagesCursor.getColumnIndex(MediaStore.Images.Media.SIZE);
            do {
                long id = imagesCursor.getLong(colId);
                String name = imagesCursor.getString(colName);
                imagesData.add(new ImageFile(id, name, imagesCursor.getLong(colSize), ContentUris.withAppendedId(imagesUri, id)));
            } while (imagesCursor.moveToNext());
            imagesCursor.close();
        }
        return imagesData;
    }
}
