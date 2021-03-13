package com.cod3rboy.apnashare.adapters;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.misc.Utilities;
import com.cod3rboy.apnashare.models.TransmissionFile;
import com.cod3rboy.apnashare.transmission.events.ProgressUpdate;
import com.cod3rboy.apnashare.viewholders.MetaFileViewHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class TransmissionFilesAdapter extends RecyclerView.Adapter<MetaFileViewHolder> {
    private Context mContext;
    private ArrayList<String> mFileUUIDs;
    private HashMap<String, TransmissionFile> mFilesToReceive;

    public TransmissionFilesAdapter(Context context) {
        mContext = context;
        mFileUUIDs = new ArrayList<>();
        mFilesToReceive = new HashMap<>();
    }

    public void setFilesToReceive(ArrayList<TransmissionFile> filesToReceive) {
        mFileUUIDs.clear();
        mFilesToReceive.clear();
        for (TransmissionFile file : filesToReceive) {
            mFileUUIDs.add(file.getFileUUID());
            mFilesToReceive.put(file.getFileUUID(), file);
        }
        notifyDataSetChanged();
    }

    public void setProgressUpdate(ProgressUpdate update) {
        int position = mFileUUIDs.indexOf(update.getFile().getFileUUID());
        if (position >= 0) {
            update.getFile().setProgressUpdate(update);
            notifyItemChanged(position);
        }
    }

    public void setFileState(TransmissionFile file, TransmissionFile.State newState) {
        int position = mFileUUIDs.indexOf(file.getFileUUID());
        if (position >= 0) {
            file.setState(newState);
            notifyItemChanged(position);
        }
    }

    public void markAllPendingMetaFilesAsFailed() {
        for (TransmissionFile file : mFilesToReceive.values()) {
            if (file.getState() != TransmissionFile.State.SUCCESS)
                file.setState(TransmissionFile.State.FAILED);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MetaFileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_meta_file, parent, false);
        MetaFileViewHolder holder = new MetaFileViewHolder(itemView);
        itemView.setOnClickListener(view -> {
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                TransmissionFile file = mFilesToReceive.get(mFileUUIDs.get(position));
                if (file == null || file.getState() != TransmissionFile.State.SUCCESS) return;
                switch (file.getCategory()) {
                    case AUDIO:
                        openAudioFile(file);
                        break;
                    case IMAGE:
                        openImageFile(file);
                        break;
                    case APP:
                        openAppInstaller(file);
                        break;
                    case VIDEO:
                        openVideoFile(file);
                        break;
                    case FILE:
                        if (!file.isDirectory())
                            openFile(file);
                }
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull MetaFileViewHolder holder, int position) {
        TransmissionFile file = mFilesToReceive.get(mFileUUIDs.get(position));
        holder.setFileName(file.getFileName());
        holder.setFileIcon(Utilities.byteArrayToBitmap(file.getFileIcon()));

        if (file.getState() == TransmissionFile.State.WAITING) {
            holder.setFillProgress(0);
            holder.setProgressInPercent("0%");
            holder.setProgressStatus(mContext.getString(R.string.meta_file_status_waiting));
        } else {
            if (file.getState() == TransmissionFile.State.PROGRESSING) {
                holder.setProgressStatus(mContext.getString(R.string.meta_file_status_receiving));
            } else if (file.getState() == TransmissionFile.State.SUCCESS) {
                holder.setProgressStatus(mContext.getString(R.string.meta_file_status_received));
            } else if (file.getState() == TransmissionFile.State.FAILED) {
                holder.setProgressStatus(mContext.getString(R.string.meta_file_status_failed));
            }
            ProgressUpdate lastUpdate = file.getProgressUpdate();
            if (lastUpdate != null) {
                holder.setFillProgress(lastUpdate.getProgress());
                holder.setProgressInPercent(lastUpdate.getProgress() + "%");
                if (file.getState() == TransmissionFile.State.PROGRESSING) {
                    if (file.isDirectory()) {
                        holder.setProgressStatus(
                                Utilities.convertFilesCountToString(lastUpdate.getBytesOrFilesProcessed()) + " / " +
                                        Utilities.convertFilesCountToString(lastUpdate.getTotalBytesOrFiles()));
                    } else {
                        holder.setProgressStatus(
                                Utilities.convertBytesToString(lastUpdate.getBytesOrFilesProcessed()) + " / " +
                                        Utilities.convertBytesToString(lastUpdate.getTotalBytesOrFiles()));
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
        return mFilesToReceive.size();
    }


    private void openAudioFile(TransmissionFile audioFile) {
        Intent target = new Intent(Intent.ACTION_VIEW);
        Uri fileUri = Utilities.getFileUri(new File(audioFile.getFilePath()));
        target.setDataAndType(fileUri, "audio/*");
        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        target.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent intent = Intent.createChooser(target, mContext.getString(R.string.chooser_title_music));
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    private void openImageFile(TransmissionFile imageFile) {
        Intent target = new Intent(Intent.ACTION_VIEW);
        Uri fileUri = Utilities.getFileUri(new File(imageFile.getFilePath()));
        target.setDataAndType(fileUri, "image/*");
        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        target.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent intent = Intent.createChooser(target, mContext.getString(R.string.chooser_title_image));
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    private void openVideoFile(TransmissionFile videoFile) {
        Intent target = new Intent(Intent.ACTION_VIEW);
        Uri fileUri = Utilities.getFileUri(new File(videoFile.getFilePath()));
        target.setDataAndType(fileUri, "video/*");
        target.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        target.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent intent = Intent.createChooser(target, mContext.getString(R.string.chooser_title_video));
        try {
            mContext.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    private void openAppInstaller(TransmissionFile appFile) {
        Intent i = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        Uri fileUri = Utilities.getFileUri(new File(appFile.getFilePath()));
        i.setData(fileUri);
        i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            mContext.startActivity(i);
        } catch (ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    private void openFile(TransmissionFile file) {
        File savedFile = new File(file.getFilePath());
        Intent openFileIntent = new Intent(Intent.ACTION_VIEW);
        // File Extension
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(savedFile.getAbsolutePath());
        if (fileExtension != null) {
            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
            openFileIntent.setType(mimeType);
        } else {
            openFileIntent.setType("*/*");
        }
        openFileIntent.setData(Utilities.getFileUri(savedFile));
        openFileIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        openFileIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent chooserIntent = Intent.createChooser(openFileIntent, mContext.getString(R.string.chooser_title_file));
        mContext.startActivity(chooserIntent);
    }
}
