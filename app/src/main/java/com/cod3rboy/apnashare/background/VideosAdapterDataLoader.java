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

import com.cod3rboy.apnashare.models.VideoFile;

public class VideosAdapterDataLoader extends AsyncTaskLoader<ArrayList<VideoFile>> {
    private static final String LOG_TAG = VideosAdapterDataLoader.class.getSimpleName();

    public VideosAdapterDataLoader(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Nullable
    @Override
    public ArrayList<VideoFile> loadInBackground() {
        ArrayList<VideoFile> videosData = new ArrayList<>();
        ContentResolver videosResolver = getContext().getContentResolver();
        Uri videosUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String querySuffixSort = String.format(Locale.getDefault(), "%s DESC", MediaStore.Video.Media._ID);
        Cursor videosCursor = videosResolver.query(
                videosUri,
                new String[]{MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.SIZE},
                null,
                null,
                querySuffixSort
        );
        if (videosCursor != null && videosCursor.moveToFirst()) {
            int colId = videosCursor.getColumnIndex(MediaStore.Video.Media._ID);
            int colName = videosCursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
            int colSize = videosCursor.getColumnIndex(MediaStore.Video.Media.SIZE);
            do {
                long id = videosCursor.getLong(colId);
                String name = videosCursor.getString(colName);
                Uri videoUri = ContentUris.withAppendedId(videosUri, id);
                videosData.add(new VideoFile(id, name, videosCursor.getLong(colSize), videoUri));
            } while (videosCursor.moveToNext());
            videosCursor.close();
        }
        return videosData;
    }
}
