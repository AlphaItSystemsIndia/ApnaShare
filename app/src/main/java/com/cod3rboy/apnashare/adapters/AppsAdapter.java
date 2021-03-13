package com.cod3rboy.apnashare.adapters;

import android.content.Context;
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
import com.cod3rboy.apnashare.models.AppFile;
import com.cod3rboy.apnashare.viewholders.AppViewHolder;

public class AppsAdapter extends RecyclerView.Adapter<AppViewHolder>
        implements ThumbnailManager.OnThumbnailListener {
    private Context mContext;
    private ArrayList<AppFile> mInstalledAppsData;
    private ArrayList<Integer> mSelectedPositions;
    private ThumbnailManager mThumbMgr;
    private int mRecyclerViewState;
    private OnFileSelectionListener mAppSelectionListener;
    private OnThumbnailAnimate mThumbnailAnimate;

    public AppsAdapter(Context context, ThumbnailManager mgr) {
        this.mContext = context;
        mInstalledAppsData = new ArrayList<>();
        mSelectedPositions = new ArrayList<>();
        mRecyclerViewState = RecyclerView.SCROLL_STATE_IDLE;
        mThumbMgr = mgr;
        mThumbMgr.addOnThumbnailListener(this);
    }

    public void setAppSelectionListener(OnFileSelectionListener listener) {
        this.mAppSelectionListener = listener;
    }

    public void setThumbnailAnimate(OnThumbnailAnimate thumbAnimate) {
        this.mThumbnailAnimate = thumbAnimate;
    }

    public void setRecyclerViewState(int state) {
        mRecyclerViewState = state;
    }

    public void setData(ArrayList<AppFile> data) {
        mInstalledAppsData.clear();
        mInstalledAppsData.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_apps, parent, false);
        AppViewHolder vh = new AppViewHolder(itemView);
        ImageView thumbnail = itemView.findViewById(R.id.iv_app_icon);
        itemView.setOnClickListener((view) -> {
            int position = vh.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                if (!mSelectedPositions.contains(position)) {
                    mSelectedPositions.add(position);
                    // Notify listener
                    if (mAppSelectionListener != null)
                        mAppSelectionListener.onFileSelected(mInstalledAppsData.get(position));
                    if (mThumbnailAnimate != null)
                        mThumbnailAnimate.thumbnailAnimate(thumbnail);

                } else {
                    mSelectedPositions.remove(Integer.valueOf(position));
                    // Notify listener
                    if (mAppSelectionListener != null)
                        mAppSelectionListener.onFileDeselected(mInstalledAppsData.get(position));
                }
                notifyItemChanged(position);
            }
        });
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppFile appInfo = mInstalledAppsData.get(position);
        holder.setAppName(appInfo.getAppName());
        holder.setAppSize(Utilities.convertBytesToString(appInfo.getBytesOrItemsCount()));
        if (mThumbMgr.isThumbnailLoaded(appInfo.getPackageName())) {
            holder.setAppIcon(appInfo.getIcon());
        } else {
            // Set placeholder thumbnail
            holder.setAppIcon(mContext.getDrawable(R.drawable.placeholder_app));
            // Load thumbnail async if recyclerview is not scrolling i.e
            // it is in either idle state or settling state.
            if (mRecyclerViewState == RecyclerView.SCROLL_STATE_IDLE ||
                    mRecyclerViewState == RecyclerView.SCROLL_STATE_SETTLING ||
                    mRecyclerViewState == RecyclerView.SCROLL_STATE_DRAGGING) {
                mThumbMgr.loadThumbnailAsync(appInfo.getPackageName(), position);
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
        return mInstalledAppsData.size();
    }

    @Override
    public void thumbnailLoaded(String mediaStoreId, int itemPosition) {
        mInstalledAppsData.get(itemPosition).setIcon(
                mThumbMgr.getThumbnail(mediaStoreId));
        notifyItemChanged(itemPosition);
    }

    public void deselectApp(AppFile selectedApp) {
        int position = mInstalledAppsData.indexOf(selectedApp);
        if (position >= 0) {
            mSelectedPositions.remove(Integer.valueOf(position));
            notifyItemChanged(position);
        }
    }
}