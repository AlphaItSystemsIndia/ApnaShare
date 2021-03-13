package com.cod3rboy.apnashare.viewholders;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;

public class AppViewHolder extends RecyclerView.ViewHolder {
    private TextView mAppNameTextView;
    private TextView mAppSizeTextView;
    private ImageView mAppIconImageView;
    private ImageView mTickImageView;

    public AppViewHolder(@NonNull View itemView) {
        super(itemView);
        mAppNameTextView = itemView.findViewById(R.id.tv_app_name);
        mAppSizeTextView = itemView.findViewById(R.id.tv_app_size);
        mAppIconImageView = itemView.findViewById(R.id.iv_app_icon);
        mTickImageView = itemView.findViewById(R.id.iv_tick);
    }

    public void setAppName(String appName){
        mAppNameTextView.setText(appName);
    }

    public void setAppSize(String appSize){
        mAppSizeTextView.setText(appSize);
    }

    public void setAppIcon(Bitmap icon){
        mAppIconImageView.setImageBitmap(icon);
    }

    public void setAppIcon(Drawable icon){
        mAppIconImageView.setImageDrawable(icon);
    }

    public void setTickVisible(boolean visible) {
        if (visible) mTickImageView.setVisibility(View.VISIBLE);
        else mTickImageView.setVisibility(View.GONE);
    }
}
