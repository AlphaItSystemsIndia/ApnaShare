package com.cod3rboy.apnashare.viewholders;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.models.BasicFile;
import com.cod3rboy.apnashare.misc.Utilities;

public class SelectedFileViewHolder extends RecyclerView.ViewHolder {
    private TextView mNameTextView;
    private TextView mSizeTextView;
    private ImageView mIconImageView;

    public SelectedFileViewHolder(@NonNull View itemView) {
        super(itemView);
        mNameTextView = itemView.findViewById(R.id.tv_file_name);
        mSizeTextView = itemView.findViewById(R.id.tv_file_size);
        mIconImageView = itemView.findViewById(R.id.iv_file_icon);
    }

    public void setData(BasicFile file) {
        mNameTextView.setText(file.getFileName());
        mSizeTextView.setText(Utilities.convertBytesToString(file.getBytesOrItemsCount()));
        mIconImageView.setImageBitmap(file.getIcon());
    }
}
