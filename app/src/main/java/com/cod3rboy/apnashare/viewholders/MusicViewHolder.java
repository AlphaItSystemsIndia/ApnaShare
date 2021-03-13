package com.cod3rboy.apnashare.viewholders;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;

public class MusicViewHolder extends RecyclerView.ViewHolder {
    private ImageView mAlbumImageView;
    private TextView mTitleTextView;
    private TextView mSizeTextView;
    private ImageView mTickImageView;

    public MusicViewHolder(@NonNull View itemView) {
        super(itemView);
        mAlbumImageView = itemView.findViewById(R.id.iv_icon_music);
        mTitleTextView = itemView.findViewById(R.id.tv_title_music);
        mSizeTextView = itemView.findViewById(R.id.tv_music_size);
        mTickImageView = itemView.findViewById(R.id.iv_tick);
    }

    public void setTitle(String title) {
        mTitleTextView.setText(title);
    }

    public void setSize(String size) {
        mSizeTextView.setText(size);
    }

    public void setAlbumArt(Bitmap albumArt) {
        mAlbumImageView.setImageBitmap(albumArt);
    }

    public void setAlbumArt(Drawable albumArt) {
        mAlbumImageView.setImageDrawable(albumArt);
    }

    public void setTickVisible(boolean visible) {
        if (visible) mTickImageView.setVisibility(View.VISIBLE);
        else mTickImageView.setVisibility(View.GONE);
    }
}
