package com.cod3rboy.apnashare.background;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;

import com.cod3rboy.apnashare.App;
import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.misc.FileSelectionQueue;
import com.cod3rboy.apnashare.models.GeneralFile;
import com.cod3rboy.apnashare.models.BasicFile;
import com.cod3rboy.apnashare.misc.Utilities;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import needle.Needle;

public class LoadFilesFromIntentTask implements Runnable {
    private static final String LOG_TAG = LoadFilesFromIntentTask.class.getSimpleName();

    public interface Callback {
        void onComplete();
    }

    private ContentResolver resolver;
    private Intent intent;
    private String intentAction;
    private FileSelectionQueue filesQueue;
    private Callback callback;

    public LoadFilesFromIntentTask(ContentResolver resolver, Intent intent, String intentAction, FileSelectionQueue filesQueue, Callback callback) {
        this.resolver = resolver;
        this.intent = intent;
        this.intentAction = intentAction;
        this.filesQueue = filesQueue;
        this.callback = callback;
    }

    @Override
    public void run() {
        if (Intent.ACTION_SEND.equals(intentAction)) {
            Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (fileUri != null) {
                BasicFile file = createFileFromUri(resolver, fileUri);
                if (file != null) filesQueue.add(file);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(intentAction)) {
            ArrayList<Uri> fileUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (fileUris != null) {
                for (Uri fileUri : fileUris) {
                    BasicFile file = createFileFromUri(resolver, fileUri);
                    if (file == null) continue;
                    filesQueue.add(file);
                }
            }
        }
        if (callback != null) {
            Needle.onMainThread().execute(() -> callback.onComplete());
        }
    }

    private BasicFile createFileFromUri(ContentResolver resolver, @NonNull Uri uri) {
        Log.d(LOG_TAG, "Creating file from URI : " + uri);
        BasicFile selectedFile = null;
        if (uri.toString().startsWith(ContentResolver.SCHEME_CONTENT)) {
            Cursor cursor = resolver.query(uri, new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, null, null, null);
            if (cursor != null && cursor.moveToNext()) {
                int colDisplayName = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int colSize = cursor.getColumnIndex(OpenableColumns.SIZE);
                String fileName = cursor.getString(colDisplayName);
                long fileSize = cursor.getLong(colSize);
                selectedFile = new GeneralFile(
                        uri,
                        fileName,
                        Utilities.drawableToBitmap(App.getInstance().getDrawable(R.drawable.ic_file)),
                        fileSize
                );
            }
            if (cursor != null) cursor.close();
        } else {
            // Some phones return absolute path Uri for file
            try {
                String filePath = uri.getPath();
                if (filePath != null) {
                    File file = new File(filePath);
                    if (!file.exists())
                        throw new FileNotFoundException("File not found at path : " + filePath);
                    if (file.isFile()) {
                        String fileName = file.getName();
                        long fileSize = file.length();
                        selectedFile = new GeneralFile(
                                uri,
                                fileName,
                                Utilities.drawableToBitmap(App.getInstance().getDrawable(R.drawable.ic_file)),
                                fileSize
                        );
                    }
                }
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        return selectedFile;
    }
}