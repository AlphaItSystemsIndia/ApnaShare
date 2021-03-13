package com.cod3rboy.apnashare.managers;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Size;

import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.misc.Utilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThumbnailManager {
    public enum ThumbnailType {
        THUMBNAIL_TYPE_APP, THUMBNAIL_TYPE_MUSIC, THUMBNAIL_TYPE_VIDEO, THUMBNAIL_TYPE_IMAGE
    }

    private static final int IMAGE_THUMBNAIL_WIDTH = 100;
    private static final int IMAGE_THUMBNAIL_HEIGHT = 100;

    private static final int VIDEO_THUMBNAIL_WIDTH = 100;
    private static final int VIDEO_THUMBNAIL_HEIGHT = 100;

    private static final int MUSIC_THUMBNAIL_WIDTH = 100;
    private static final int MUSIC_THUMBNAIL_HEIGHT = 100;

    public interface OnThumbnailListener {
        void thumbnailLoaded(String mediaStoreId, int itemPosition);
    }

    private Context mContext;
    private ExecutorService mThreadExecutor;
    private final HashMap<String, Bitmap> mThumbnailIdsMap;
    private ThumbnailType mThumbType;
    private ArrayList<OnThumbnailListener> mListeners;
    private Handler mMainThreadHandler;


    public ThumbnailManager(Context context, ThumbnailType thumbType) {
        mContext = context;
        mThreadExecutor = Executors.newSingleThreadExecutor();
        mThumbnailIdsMap = new HashMap<>();
        mListeners = new ArrayList<>();
        mThumbType = thumbType;
        mMainThreadHandler = new Handler(Looper.getMainLooper());
    }

    public boolean isThumbnailLoaded(String itemId) {
        boolean loaded;
        synchronized (mThumbnailIdsMap) {
            loaded = mThumbnailIdsMap.containsKey(itemId);
        }
        return loaded;
    }

    public Bitmap getThumbnail(String itemId) {
        Bitmap thumb;
        synchronized (mThumbnailIdsMap) {
            thumb = mThumbnailIdsMap.get(itemId);
        }
        return thumb;
    }

    public void addOnThumbnailListener(OnThumbnailListener listener) {
        if (listener != null) mListeners.add(listener);
    }

    public void removeOnThumbnailListener(OnThumbnailListener listener) {
        mListeners.remove(listener);
    }

    public void loadThumbnailAsync(String itemId, int itemPosition) {
        switch (mThumbType) {
            case THUMBNAIL_TYPE_IMAGE:
                loadImageThumbnail(itemId, itemPosition);
                break;
            case THUMBNAIL_TYPE_VIDEO:
                loadVideoThumbnail(itemId, itemPosition);
                break;
            case THUMBNAIL_TYPE_APP:
                loadAppThumbnail(itemId, itemPosition);
                break;
            case THUMBNAIL_TYPE_MUSIC:
                loadMusicThumbnail(itemId, itemPosition);
                break;
        }
    }

    private void loadImageThumbnail(String mediaStoreId, int itemPosition) {
        mThreadExecutor.execute(() -> {
            long id = Long.parseLong(mediaStoreId);
            Uri imgUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            ContentResolver imagesResolver = mContext.getContentResolver();
            Bitmap thumbnail = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    thumbnail = imagesResolver.loadThumbnail(imgUri, new Size(IMAGE_THUMBNAIL_WIDTH, IMAGE_THUMBNAIL_HEIGHT), null);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                int imgKind = MediaStore.Images.Thumbnails.MICRO_KIND;
                BitmapFactory.Options imgOptions = new BitmapFactory.Options();
                thumbnail = MediaStore.Images.Thumbnails.getThumbnail(imagesResolver, id, imgKind, imgOptions);
            }

            if (thumbnail == null) { // Fall back to Default Thumbnail Image
                thumbnail = Utilities.drawableToBitmap(mContext.getDrawable(R.drawable.placeholder_image));
            }
            synchronized (mThumbnailIdsMap) {
                // This also replaces any old thumbnail so it is the responsibility of caller
                // to ensure that thumbnail does not already exist for given id before loading thumbnail.
                mThumbnailIdsMap.put(mediaStoreId, thumbnail);
            }
            notifyListeners(mediaStoreId, itemPosition);
        });
    }

    private void loadVideoThumbnail(String mediaStoreId, int itemPosition) {
        mThreadExecutor.execute(() -> {
            long id = Long.parseLong(mediaStoreId);
            ContentResolver videosResolver = mContext.getContentResolver();
            Uri videoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
            Bitmap thumbnail = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    thumbnail = videosResolver.loadThumbnail(videoUri, new Size(VIDEO_THUMBNAIL_WIDTH, VIDEO_THUMBNAIL_HEIGHT), null);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } else {
                int thumbKind = MediaStore.Video.Thumbnails.MICRO_KIND;
                BitmapFactory.Options thumbOptions = new BitmapFactory.Options();
                thumbnail = MediaStore.Video.Thumbnails.getThumbnail(videosResolver, id, thumbKind, thumbOptions);
            }
            if (thumbnail == null) { // Fallback to Default Thumbnail Image
                thumbnail = Utilities.drawableToBitmap(mContext.getDrawable(R.drawable.placeholder_video));
            }

            synchronized (mThumbnailIdsMap) {
                // This also replaces any old thumbnail so it is the responsibility of caller
                // to ensure that thumbnail does not already exist for given id before loading thumbnail.
                mThumbnailIdsMap.put(mediaStoreId, thumbnail);
            }
            notifyListeners(mediaStoreId, itemPosition);
        });
    }

    private void loadAppThumbnail(String appPackage, int itemPosition) {
        mThreadExecutor.execute(() -> {
            Bitmap appIcon = null;
            try {
                appIcon = Utilities.drawableToBitmap(mContext.getPackageManager().getApplicationIcon(appPackage));
            } catch (PackageManager.NameNotFoundException ex) {
                ex.printStackTrace();
                // Fallback to default app icon
                appIcon = Utilities.drawableToBitmap(mContext.getDrawable(R.drawable.placeholder_app));
            }
            synchronized (mThumbnailIdsMap) {
                // This also replaces any old thumbnail so it is the responsibility of caller
                // to ensure that thumbnail does not already exist for given id before loading thumbnail.
                mThumbnailIdsMap.put(appPackage, appIcon);
            }
            notifyListeners(appPackage, itemPosition);
        });
    }

    private void loadMusicThumbnail(String mediaStoreId, int itemPosition) {
        mThreadExecutor.execute(() -> {
            Bitmap albumArt = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    long id = Long.parseLong(mediaStoreId);
                    Uri musicUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                    ContentResolver musicResolver = mContext.getContentResolver();
                    albumArt = musicResolver.loadThumbnail(musicUri, new Size(MUSIC_THUMBNAIL_WIDTH, MUSIC_THUMBNAIL_HEIGHT), null);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (albumArt == null) { // Fallback to Default Album Art image
                albumArt = Utilities.drawableToBitmap(mContext.getDrawable(R.drawable.placeholder_music));
            }
            synchronized (mThumbnailIdsMap) {
                // This also replaces any old thumbnail so it is the responsibility of caller
                // to ensure that thumbnail does not already exist for given id before loading thumbnail.
                mThumbnailIdsMap.put(mediaStoreId, albumArt);
            }
            notifyListeners(mediaStoreId, itemPosition);
        });

    }

    private void notifyListeners(String itemId, int itemPosition) {
        mMainThreadHandler.post(() -> {
            for (int i = 0; i < mListeners.size(); i++)
                mListeners.get(i).thumbnailLoaded(itemId, itemPosition);
        });
    }

    public void dispose() {
        mThreadExecutor.shutdown();
    }
}
