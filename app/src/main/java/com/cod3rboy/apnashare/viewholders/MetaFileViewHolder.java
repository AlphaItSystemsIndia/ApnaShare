package com.cod3rboy.apnashare.viewholders;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;
import com.devzone.fillprogresslayout.FillProgressLayout;

public class MetaFileViewHolder extends RecyclerView.ViewHolder {
    private FillProgressLayout mFillProgressView;
    private ImageView mFileIconView;
    private TextView mFileNameView;
    private TextView mProgressStatus;
    private TextView mProgressPercent;

    public MetaFileViewHolder(@NonNull View itemView) {
        super(itemView);
        mFillProgressView = itemView.findViewById(R.id.progress_fill);
        mFileIconView = itemView.findViewById(R.id.iv_file_icon);
        mFileNameView = itemView.findViewById(R.id.tv_file_name);
        mProgressStatus = itemView.findViewById(R.id.tv_progress_status);
        mProgressPercent = itemView.findViewById(R.id.tv_progress_percent);
    }

    public void setFillProgress(int value) {
        mFillProgressView.setProgress(value, false);
    }

    public void setFileIcon(Drawable icon) {
        mFileIconView.setImageDrawable(icon);
    }

    public void setFileIcon(Bitmap icon) {
        mFileIconView.setImageBitmap(icon);
    }

    public void setFileName(String fileName) {
        mFileNameView.setText(fileName);
    }

    public void setProgressStatus(String status) {
        mProgressStatus.setText(status);
    }

    public void setProgressInPercent(String percentProgress) {
        mProgressPercent.setText(percentProgress);
    }
}
