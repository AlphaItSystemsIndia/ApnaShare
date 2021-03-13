package com.cod3rboy.apnashare.adapters;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;

import java.util.ArrayList;

import com.cod3rboy.apnashare.interfaces.OnFileSelectionListener;
import com.cod3rboy.apnashare.interfaces.OnThumbnailAnimate;
import com.cod3rboy.apnashare.managers.ThumbnailManager;
import com.cod3rboy.apnashare.misc.Utilities;
import com.cod3rboy.apnashare.models.AudioFile;
import com.cod3rboy.apnashare.viewholders.MusicViewHolder;

public class MusicAdapter extends RecyclerView.Adapter<MusicViewHolder>
        implements ThumbnailManager.OnThumbnailListener {
    private Context mContext;
    private ArrayList<AudioFile> mMusicData;
    private ArrayList<Integer> mSelectedPositions;
    private ThumbnailManager mThumbMgr;
    private int mRecyclerViewState;
    private OnFileSelectionListener mMusicSelectionListener;
    private OnThumbnailAnimate mThumbnailAnimate;

    public MusicAdapter(Context context, ThumbnailManager thumbnailManager) {
        this.mContext = context;
        this.mMusicData = new ArrayList<>();
        this.mSelectedPositions = new ArrayList<>();
        this.mThumbMgr = thumbnailManager;
        this.mRecyclerViewState = RecyclerView.SCROLL_STATE_IDLE;
        this.mThumbMgr.addOnThumbnailListener(this);
    }

    public void setMusicSelectionListener(OnFileSelectionListener listener) {
        this.mMusicSelectionListener = listener;
    }

    public void setThumbnailAnimate(OnThumbnailAnimate thumbAnimate) {
        this.mThumbnailAnimate = thumbAnimate;
    }

    public void setRecyclerViewState(int newState) {
        this.mRecyclerViewState = newState;
    }

    public void setData(ArrayList<AudioFile> data) {
        mMusicData.clear();
        mMusicData.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_music, parent, false);
        MusicViewHolder vh = new MusicViewHolder(itemView);
        ImageView thumbnail = itemView.findViewById(R.id.iv_icon_music);
        itemView.setOnClickListener((view) -> {
            int position = vh.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                if (!mSelectedPositions.contains(position)) {
                    mSelectedPositions.add(position);
                    // Notify Listeners
                    if (mMusicSelectionListener != null)
                        mMusicSelectionListener.onFileSelected(mMusicData.get(position));
                    if (mThumbnailAnimate != null)
                        mThumbnailAnimate.thumbnailAnimate(thumbnail);
                } else {
                    mSelectedPositions.remove(Integer.valueOf(position));
                    // Notify Listeners
                    if (mMusicSelectionListener != null)
                        mMusicSelectionListener.onFileDeselected(mMusicData.get(position));
                }
                notifyItemChanged(position);
            }
        });
        itemView.setOnLongClickListener(view -> {
            int position = vh.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION)
                return openMusicFile(mMusicData.get(position));
            return false;
        });
        return vh;
    }

    private boolean openMusicFile(AudioFile mf) {
        Intent target = new Intent(Intent.ACTION_VIEW);
        target.setDataAndType(mf.getMediaStoreUri(), "audio/*");
        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        Intent intent = Intent.createChooser(target, mContext.getString(R.string.chooser_title_music));
        try {
            mContext.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        AudioFile audioFile = mMusicData.get(position);
        holder.setTitle(audioFile.getFileName());
        holder.setSize(Utilities.convertBytesToString(audioFile.getBytesOrItemsCount()));
        String musicMediaStoreId = String.valueOf(audioFile.getMediaStoreId());
        if (mThumbMgr.isThumbnailLoaded(musicMediaStoreId)) {
            holder.setAlbumArt(audioFile.getIcon());
        } else {
            // Set placeholder album art cover
            holder.setAlbumArt(mContext.getDrawable(R.drawable.placeholder_music));
            // Load thumbnail async if recyclerview is not scrolling i.e
            // it is in either idle state or settling state.
            if (mRecyclerViewState == RecyclerView.SCROLL_STATE_IDLE ||
                    mRecyclerViewState == RecyclerView.SCROLL_STATE_SETTLING ||
                    mRecyclerViewState == RecyclerView.SCROLL_STATE_DRAGGING) {
                mThumbMgr.loadThumbnailAsync(musicMediaStoreId, position);
            }
        }

        if (mSelectedPositions.contains(position)) holder.setTickVisible(true);
        else holder.setTickVisible(false);
    }

    @Override
    public int getItemCount() {
        return mMusicData.size();
    }

    @Override
    public void thumbnailLoaded(String mediaStoreId, int itemPosition) {
        mMusicData.get(itemPosition).setIcon(mThumbMgr.getThumbnail(mediaStoreId));
        notifyItemChanged(itemPosition);
    }

    public void deselectMusic(AudioFile selectedMusic) {
        int position = mMusicData.indexOf(selectedMusic);
        if (position >= 0) {
            mSelectedPositions.remove(Integer.valueOf(position));
            notifyItemChanged(position);
        }
    }
}
