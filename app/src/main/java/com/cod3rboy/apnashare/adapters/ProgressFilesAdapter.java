package com.cod3rboy.apnashare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.misc.Utilities;
import com.cod3rboy.apnashare.models.TransmissionFile;
import com.cod3rboy.apnashare.transmission.events.ProgressUpdate;
import com.cod3rboy.apnashare.viewholders.ProgressFileViewHolder;

import java.util.ArrayList;

public class ProgressFilesAdapter extends RecyclerView.Adapter<ProgressFileViewHolder> {
    private Context mContext;
    private ArrayList<TransmissionFile> mFilesInProgress;

    public ProgressFilesAdapter(Context context) {
        mContext = context;
        mFilesInProgress = new ArrayList<>();
    }

    public void setFilesInProgress(ArrayList<TransmissionFile> files) {
        mFilesInProgress.clear();
        mFilesInProgress.addAll(files);
        notifyDataSetChanged();
    }

    public void updateProgressFile(ProgressUpdate update) {
        // Find index of TransmissionFile for which update object is received
        int index = mFilesInProgress.indexOf(update.getFile());
        if (index >= 0) {
            // Found Index
            mFilesInProgress.get(index).setProgressUpdate(update);
            notifyItemChanged(index);
        }
    }

    public void changeFileState(TransmissionFile file, TransmissionFile.State newState) {
        // Find index of TransmissionFile whose state should be updated
        int index = mFilesInProgress.indexOf(file);
        if (index >= 0) {
            // Found Index
            file.setState(newState);
            notifyItemChanged(index);
        }
    }

    public void markAllPendingFilesAsFailed() {
        for (TransmissionFile file : mFilesInProgress) {
            if (file.getState() != TransmissionFile.State.SUCCESS)
                file.setState(TransmissionFile.State.FAILED);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProgressFileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_progress_file, parent, false);
        ProgressFileViewHolder holder = new ProgressFileViewHolder(itemView);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ProgressFileViewHolder holder, int position) {
        TransmissionFile transmissionFile = mFilesInProgress.get(position);
        holder.setFileName(transmissionFile.getFileName());
        holder.setFileIcon(Utilities.byteArrayToBitmap(transmissionFile.getFileIcon()));
        if (transmissionFile.getState() == TransmissionFile.State.WAITING) {
            holder.setProgressStatus(mContext.getString(R.string.file_progress_status_waiting));
            holder.setProgressInPercent("0%");
            holder.setFillProgress(0);
        } else {
            if (transmissionFile.getState() == TransmissionFile.State.PROGRESSING) {
                holder.setProgressStatus(mContext.getString(R.string.file_progress_status_sending));
            } else if (transmissionFile.getState() == TransmissionFile.State.SUCCESS) {
                holder.setProgressStatus(mContext.getString(R.string.file_progress_status_sent));
            } else if (transmissionFile.getState() == TransmissionFile.State.FAILED) {
                holder.setProgressStatus(mContext.getString(R.string.file_progress_status_failed));
            }
            ProgressUpdate lastUpdate = transmissionFile.getProgressUpdate();
            if (lastUpdate != null) {
                holder.setFillProgress(lastUpdate.getProgress());
                holder.setProgressInPercent(lastUpdate.getProgress() + "%");
                if (transmissionFile.getState() == TransmissionFile.State.PROGRESSING) {
                    if (transmissionFile.isDirectory()) {
                        holder.setProgressStatus(Utilities.convertFilesCountToString(lastUpdate.getBytesOrFilesProcessed()) +
                                " / " + Utilities.convertFilesCountToString(lastUpdate.getTotalBytesOrFiles()));
                    } else {
                        holder.setProgressStatus(Utilities.convertBytesToString(lastUpdate.getBytesOrFilesProcessed()) +
                                " / " + Utilities.convertBytesToString(lastUpdate.getTotalBytesOrFiles()));
                    }
                }
            } else {
                holder.setFillProgress(0);
                holder.setProgressInPercent("0%");
            }
        }
    }

    @Override
    public int getItemCount() {
        return mFilesInProgress.size();
    }
}
