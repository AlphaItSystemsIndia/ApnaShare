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

import com.cod3rboy.apnashare.models.AudioFile;

public class MusicAdapterDataLoader extends AsyncTaskLoader<ArrayList<AudioFile>> {

    public MusicAdapterDataLoader(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Nullable
    @Override
    public ArrayList<AudioFile> loadInBackground() {
        ArrayList<AudioFile> musicData = new ArrayList<>();
        ContentResolver musicResolver = getContext().getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String querySuffixSort = String.format(Locale.getDefault(), "%s DESC", MediaStore.Audio.Media._ID);
        Cursor musicCursor = musicResolver.query(musicUri,
                new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.SIZE},
                null,
                null,
                querySuffixSort
        );
        if (musicCursor != null && musicCursor.moveToFirst()) {
            // Create column indexes
            int colId = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int colDisplayName = musicCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
            int colSize = musicCursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
            do {
                long musicId = musicCursor.getLong(colId);
                String musicDisplayName = musicCursor.getString(colDisplayName);
                // skip those music files whose display name is null. @todo find alternative to this
                if (musicDisplayName == null) continue;
                musicData.add(new AudioFile(musicId, musicDisplayName, musicCursor.getLong(colSize), ContentUris.withAppendedId(musicUri, musicId)));
            } while (musicCursor.moveToNext());
            musicCursor.close();
        }
        return musicData;
    }
}
