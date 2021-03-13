package com.cod3rboy.apnashare.viewholders;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;

public class ImageViewHolder extends RecyclerView.ViewHolder {

    private ImageView mThumbImageView;
    private ImageView mTickImageView;

    public ImageViewHolder(@NonNull View itemView) {
        super(itemView);
        mThumbImageView = itemView.findViewById(R.id.iv_image_thumb);
        mTickImageView = itemView.findViewById(R.id.iv_tick);
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
