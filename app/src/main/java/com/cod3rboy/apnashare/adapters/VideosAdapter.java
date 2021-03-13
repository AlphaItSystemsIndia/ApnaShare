package com.cod3rboy.apnashare.adapters;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;

import java.io.IOException;
import java.util.ArrayList;

import com.cod3rboy.apnashare.interfaces.OnFileSelectionListener;
import com.cod3rboy.apnashare.interfaces.OnThumbnailAnimate;
import com.cod3rboy.apnashare.managers.ThumbnailManager;
import com.cod3rboy.apnashare.misc.Utilities;
import com.cod3rboy.apnashare.models.VideoFile;
import com.cod3rboy.apnashare.viewholders.VideoViewHolder;

import needle.Needle;

public class VideosAdapter extends RecyclerView.Adapter<VideoViewHolder>
        implements ThumbnailManager.OnThumbnailListener {
    private Context mContext;

    private ArrayList<VideoFile> mVideosData;
    private ArrayList<Integer> mSelectedPositions;
    private ThumbnailManager mThumbMgr;
    private int mRecyclerViewState;
    private OnFileSelectionListener mVideoSelectionListener;
    private OnThumbnailAnimate mThumbnailAnimate;

    public VideosAdapter(Context context, ThumbnailManager thumbnailManager) {
        mContext = context;
        mVideosData = new ArrayList<>();
        mSelectedPositions = new ArrayList<>();
        mThumbMgr = thumbnailManager;
        mRecyclerViewState = RecyclerView.SCROLL_STATE_IDLE;
        mThumbMgr.addOnThumbnailListener(this);
    }

    public void setVideoSelectionListener(OnFileSelectionListener listener) {
        this.mVideoSelectionListener = listener;
    }

    public void setThumbnailAnimate(OnThumbnailAnimate thumbnailAnimate) {
        mThumbnailAnimate = thumbnailAnimate;
    }

    public void setRecyclerViewState(int newState) {
        mRecyclerViewState = newState;
    }

    public void setData(ArrayList<VideoFile> data) {
        mVideosData.clear();
        mVideosData.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_video, parent, false);
        VideoViewHolder holder = new VideoViewHolder(itemView);
        ImageView thumbnail = itemView.findViewById(R.id.iv_video_thumb);
        itemView.setOnClickListener((view) -> {
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                if (!mSelectedPositions.contains(position)) {
                    mSelectedPositions.add(position);
                    // Notify listener
                    if (mVideoSelectionListener != null)
                        mVideoSelectionListener.onFileSelected(mVideosData.get(position));
                    if (mThumbnailAnimate != null)
                        mThumbnailAnimate.thumbnailAnimate(thumbnail);
                } else {
                    mSelectedPositions.remove(Integer.valueOf(position));
                    // Notify listener
                    if (mVideoSelectionListener != null)
                        mVideoSelectionListener.onFileDeselected(mVideosData.get(position));
                }
                notifyItemChanged(position);
            }
        });
        itemView.setOnLongClickListener(view -> {
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION)
                return openVideoFile(mVideosData.get(position));
            return false;
        });
        return holder;
    }

    private boolean openVideoFile(VideoFile videoFile) {
        Intent target = new Intent(Intent.ACTION_VIEW);
        target.setDataAndType(videoFile.getMediaStoreUri(), "video/*");
        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        Intent intent = Intent.createChooser(target, mContext.getString(R.string.chooser_title_video));
        try {
            mContext.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoFile videoFile = mVideosData.get(position);
        String mediaStoreId = String.valueOf(videoFile.getMediaStoreId());
        if (mThumbMgr.isThumbnailLoaded(mediaStoreId)) {
            holder.setThumbnail(videoFile.getIcon());
            holder.setPlayIconVisible(true);
        } else {
            // Set placeholder thumbnail
            holder.setThumbnail(mContext.getDrawable(R.drawable.placeholder_video));
            holder.setPlayIconVisible(false);
            // Load thumbnail async if recyclerview is not scrolling i.e
            // it is in either idle state or settling state.
            if (mRecyclerViewState == RecyclerView.SCROLL_STATE_IDLE ||
                    mRecyclerViewState == RecyclerView.SCROLL_STATE_SETTLING ||
                    mRecyclerViewState == RecyclerView.SCROLL_STATE_DRAGGING) {
                mThumbMgr.loadThumbnailAsync(mediaStoreId, position);
            }
        }
        // Set Video Duration
        if (videoFile.getDuration() < 0) {
            // Video Duration is not loaded yet
            if (mRecyclerViewState == RecyclerView.SCROLL_STATE_IDLE ||
                    mRecyclerViewState == RecyclerView.SCROLL_STATE_SETTLING ||
                    mRecyclerViewState == RecyclerView.SCROLL_STATE_DRAGGING) {
                loadVideoDurationAsync(mContext, videoFile, position);
                videoFile.setDuration(0); // Enforce one time load operation
            }
            holder.setVideoDuration(Utilities.formatVideoDuration(0));
        } else {
            holder.setVideoDuration(Utilities.formatVideoDuration(videoFile.getDuration()));
        }
        if (mSelectedPositions.contains(position)) {
            holder.setTickVisible(true);
        } else {
            holder.setTickVisible(false);
        }
    }

    private void loadVideoDurationAsync(final Context context, final VideoFile videoFile, final int position) {
        Needle.onBackgroundThread().withThreadPoolSize(2).execute(() -> {
            MediaMetadataRetriever retriever = null;
            ParcelFileDescriptor parcelFileDesc = null;
            try {
                retriever = new MediaMetadataRetriever();
                parcelFileDesc = context.getContentResolver().openFileDescriptor(videoFile.getMediaStoreUri(), "r");
                retriever.setDataSource(parcelFileDesc.getFileDescriptor());
                long duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                if (duration < 0) duration = 0;
                videoFile.setDuration(duration);
                Needle.onMainThread().execute(() -> notifyItemChanged(position));
            } catch (IOException | RuntimeException ex) {
                ex.printStackTrace();
            } finally {
                if (retriever != null) {
                    retriever.release();
                }
                if (parcelFileDesc != null) {
                    try {
                        parcelFileDesc.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mVideosData.size();
    }

    @Override
    public void thumbnailLoaded(String mediaStoreId, int itemPosition) {
        mVideosData.get(itemPosition).setIcon(
                mThumbMgr.getThumbnail(mediaStoreId));
        notifyItemChanged(itemPosition);
    }

    public void deselectVideo(VideoFile selectedVideo) {
        int position = mVideosData.indexOf(selectedVideo);
        if (position >= 0) {
            mSelectedPositions.remove(Integer.valueOf(position));
            notifyItemChanged(position);
        }
    }
}
