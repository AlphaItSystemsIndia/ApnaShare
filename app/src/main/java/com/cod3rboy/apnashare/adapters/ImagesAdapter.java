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
import com.cod3rboy.apnashare.models.ImageFile;
import com.cod3rboy.apnashare.viewholders.ImageViewHolder;

public class ImagesAdapter extends RecyclerView.Adapter<ImageViewHolder>
        implements ThumbnailManager.OnThumbnailListener {
    private Context mContext;
    private ArrayList<ImageFile> mImagesData;
    private ArrayList<Integer> mSelectedPositions;
    private ThumbnailManager mThumbMgr;
    private int mRecyclerViewState;
    private OnFileSelectionListener mImageSelectionListener;
    private OnThumbnailAnimate mThumbnailAnimate;

    public ImagesAdapter(Context context, ThumbnailManager thumbnailManager) {
        mContext = context;
        mImagesData = new ArrayList<>();
        mSelectedPositions = new ArrayList<>();
        mThumbMgr = thumbnailManager;
        mThumbMgr.addOnThumbnailListener(this);
        mRecyclerViewState = RecyclerView.SCROLL_STATE_IDLE;
    }

    public void setImageSelectionListener(OnFileSelectionListener listener) {
        this.mImageSelectionListener = listener;
    }

    public void setThumbnailAnimate(OnThumbnailAnimate thumbnailAnimate) {
        mThumbnailAnimate = thumbnailAnimate;
    }

    public void setData(ArrayList<ImageFile> data) {
        mImagesData.clear();
        mImagesData.addAll(data);
        notifyDataSetChanged();
    }

    public void setRecyclerViewState(int newState) {
        mRecyclerViewState = newState;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_image, parent, false);
        ImageViewHolder vh = new ImageViewHolder(itemView);
        ImageView thumbnail = itemView.findViewById(R.id.iv_image_thumb);
        itemView.setOnClickListener((view) -> {
            int position = vh.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                if (!mSelectedPositions.contains(position)) {
                    mSelectedPositions.add(position);
                    // Notify Listener
                    if (mImageSelectionListener != null)
                        mImageSelectionListener.onFileSelected(mImagesData.get(position));
                    if (mThumbnailAnimate != null)
                        mThumbnailAnimate.thumbnailAnimate(thumbnail);
                } else {
                    mSelectedPositions.remove(Integer.valueOf(position));
                    // Notify Listener
                    if (mImageSelectionListener != null)
                        mImageSelectionListener.onFileDeselected(mImagesData.get(position));
                }
                notifyItemChanged(position);
            }
        });
        itemView.setOnLongClickListener(view -> {
            int position = vh.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION)
                return openImageFile(mImagesData.get(position));
            return false;
        });
        return vh;
    }

    private boolean openImageFile(ImageFile imageFile) {
        Intent target = new Intent(Intent.ACTION_VIEW);
        target.setDataAndType(imageFile.getMediaStoreUri(), "image/*");
        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        Intent intent = Intent.createChooser(target, mContext.getString(R.string.chooser_title_image));
        try {
            mContext.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        // Set thumbnail for items
        ImageFile imgInfo = mImagesData.get(position);
        String mediaStoreId = String.valueOf(imgInfo.getMediaStoreId());
        if (mThumbMgr.isThumbnailLoaded(mediaStoreId)) {
            holder.setThumbnail(imgInfo.getIcon());
        } else {
            // Set placeholder thumbnail
            holder.setThumbnail(mContext.getDrawable(R.drawable.placeholder_image));
            // Load thumbnail async if recyclerview is not scrolling i.e
            // it is in either idle state or settling state.
            if (mRecyclerViewState == RecyclerView.SCROLL_STATE_IDLE ||
                    mRecyclerViewState == RecyclerView.SCROLL_STATE_SETTLING ||
                    mRecyclerViewState == RecyclerView.SCROLL_STATE_DRAGGING) {
                mThumbMgr.loadThumbnailAsync(mediaStoreId, position);
            }
        }
        if (mSelectedPositions.contains(position)) {
            holder.setTickVisible(true);
        } else {
            holder.setTickVisible(false);
        }
    }

    @Override
    public int getItemCount() {
        return mImagesData.size();
    }

    @Override
    public long getItemId(int position) {
        return mImagesData.get(position).getMediaStoreId();
    }

    @Override
    public void thumbnailLoaded(String mediaStoreId, int itemPosition) {
        mImagesData.get(itemPosition).setIcon(
                mThumbMgr.getThumbnail(mediaStoreId));
        notifyItemChanged(itemPosition);
    }

    public void deselectImage(ImageFile selectedImage) {
        int position = mImagesData.indexOf(selectedImage);
        if (position >= 0) {
            mSelectedPositions.remove(Integer.valueOf(position));
            notifyItemChanged(position);
        }
    }
}
