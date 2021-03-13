package com.cod3rboy.apnashare.viewholders;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;

public class VideoViewHolder extends RecyclerView.ViewHolder {
    private ImageView mThumbImageView;
    private ImageView mTickImageView;
    private ImageView mPlayIcon;
    private TextView mTextViewDuration;

    public VideoViewHolder(@NonNull View itemView) {
        super(itemView);
        mThumbImageView = itemView.findViewById(R.id.iv_video_thumb);
        mTickImageView = itemView.findViewById(R.id.iv_tick);
        mPlayIcon = itemView.findViewById(R.id.icon_play);
        mTextViewDuration = itemView.findViewById(R.id.tv_duration);
    }

    public void setPlayIconVisible(boolean visible) {
        if (visible) mPlayIcon.setVisibility(View.VISIBLE);
        else mPlayIcon.setVisibility(View.GONE);
    }

    public void setVideoDuration(String duration) {
        mTextViewDuration.setText(duration);
    }

    public void setTickVisible(boolean visible) {
        if (visible) mTickImageView.setVisibility(View.VISIBLE);
        else mTickImageView.setVisibility(View.GONE);
    }

    public void setThumbnail(Bitmap thumbnail) {
        mThumbImageView.setImageBitmap(thumbnail);
    }

    public void setThumbnail(Drawable thumbnail) {
        mThumbImageView.setImageDrawable(thumbnail);
    }
}
