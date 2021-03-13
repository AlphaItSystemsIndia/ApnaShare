package com.cod3rboy.apnashare.viewholders;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;

public class FileSystemEntryViewHolder extends RecyclerView.ViewHolder {
    private ImageView mIconImageView;
    private TextView mNameTextView;
    private TextView mDescTextView;
    private ImageView mSelectionIcon;

    public FileSystemEntryViewHolder(@NonNull View itemView) {
        super(itemView);
        mIconImageView = itemView.findViewById(R.id.iv_icon_item);
        mNameTextView = itemView.findViewById(R.id.tv_item_name);
        mDescTextView = itemView.findViewById(R.id.tv_item_desc);
        mSelectionIcon = itemView.findViewById(R.id.iv_selection);
    }

    public void setIcon(Bitmap icon) {
        mIconImageView.setImageBitmap(icon);
    }
    public void setIcon(Drawable icon) {
        mIconImageView.setImageDrawable(icon);
    }

    public void setName(String name) {
        mNameTextView.setText(name);
    }

    public void setDescription(String description) {
        mDescTextView.setText(description);
    }

    public void setSelectionIcon(Drawable checkedIcon) {
        mSelectionIcon.setImageDrawable(checkedIcon);
    }

    public void setSelectionIconVisibility(boolean visible) {
        if (visible) mSelectionIcon.setVisibility(View.VISIBLE);
        else mSelectionIcon.setVisibility(View.GONE);
    }
}
