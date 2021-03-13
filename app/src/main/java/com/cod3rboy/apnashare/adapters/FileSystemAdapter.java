package com.cod3rboy.apnashare.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.interfaces.OnFileSelectionListener;
import com.cod3rboy.apnashare.interfaces.OnThumbnailAnimate;
import com.cod3rboy.apnashare.managers.FileManager;
import com.cod3rboy.apnashare.models.GeneralFile;
import com.cod3rboy.apnashare.misc.Utilities;
import com.cod3rboy.apnashare.viewholders.FileSystemEntryViewHolder;

import java.io.File;
import java.util.ArrayList;

public class FileSystemAdapter extends RecyclerView.Adapter<FileSystemEntryViewHolder> {

    private Context mContext;
    private ArrayList<String> mSelectedFilePaths;
    private FileManager mFileManager;
    private OnFileSelectionListener mFileSelectionListener;
    private OnThumbnailAnimate mThumbnailAnimate;

    public FileSystemAdapter(Context context, FileManager fileManager) {
        this.mContext = context;
        this.mFileManager = fileManager;
        this.mSelectedFilePaths = new ArrayList<>();
    }

    public void setFileSelectionListener(OnFileSelectionListener listener) {
        this.mFileSelectionListener = listener;
    }

    public void setThumbnailAnimate(OnThumbnailAnimate thumbnailAnimate) {
        mThumbnailAnimate = thumbnailAnimate;
    }

    @NonNull
    @Override
    public FileSystemEntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_filesytem_entry, parent, false);
        FileSystemEntryViewHolder holder = new FileSystemEntryViewHolder(itemView);
        itemView.setOnClickListener((view) -> {
            int position = holder.getAdapterPosition();
            onItemViewClicked(position, holder, view);
        });
        itemView.setOnLongClickListener(view -> {
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return false;
            File file = mFileManager.getFileAtIndex(position);
            if (file.isDirectory()) {
                toggleDirectorySelection(holder.getAdapterPosition(), holder, view);
            } else {
                // Open Long clicked file
                Intent openFileIntent = new Intent(Intent.ACTION_VIEW);
                // File Extension
                String fileExtension = MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath());
                if (fileExtension != null) {
                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
                    openFileIntent.setType(mimeType);
                } else {
                    openFileIntent.setType("*/*");
                }
                openFileIntent.setData(Utilities.getFileUri(file));
                openFileIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                openFileIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Intent chooserIntent = Intent.createChooser(openFileIntent, mContext.getString(R.string.chooser_title_file));
                mContext.startActivity(chooserIntent);
            }
            return true;
        });
        itemView.findViewById(R.id.iv_selection).setOnClickListener((view) -> {
            toggleDirectorySelection(holder.getAdapterPosition(), holder, itemView);
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull FileSystemEntryViewHolder holder, int position) {
        File file = mFileManager.getFileAtIndex(position);
        if (position > 0) {
            holder.setName(file.getName());
            if (file.isDirectory()) {
                holder.setSelectionIconVisibility(true);
                holder.setDescription(Utilities.convertFolderItemsToString(mFileManager.getChildCountAtIndex(position)));
                holder.setIcon(mContext.getDrawable(R.drawable.ic_folder_closed));
            } else {
                holder.setSelectionIconVisibility(false);
                holder.setDescription(Utilities.convertBytesToString(file.length()));
                holder.setIcon(mContext.getDrawable(R.drawable.ic_file));
            }
            String filePath = mFileManager.getFileAtIndex(position).getAbsolutePath();
            if (mSelectedFilePaths.contains(filePath)) {
                holder.setSelectionIconVisibility(true);
                holder.setSelectionIcon(mContext.getDrawable(R.drawable.ic_checked));
            } else {
                holder.setSelectionIcon(mContext.getDrawable(R.drawable.ic_unchecked));
            }
        } else {
            holder.setName(mContext.getString(R.string.dir_title_back));
            holder.setDescription(mContext.getString(R.string.dir_desc_back));
            holder.setIcon(mContext.getDrawable(R.drawable.ic_folder_opened));
            holder.setSelectionIconVisibility(false);
        }
    }

    @Override
    public int getItemCount() {
        return mFileManager.getActiveDirectoryListingCount();
    }

    private void onItemViewClicked(int position, FileSystemEntryViewHolder viewHolder, View itemView) {
        if (position == RecyclerView.NO_POSITION) return;
        File file = mFileManager.getFileAtIndex(position);
        if (file.isDirectory()) {
            // visit this directory
            mFileManager.visitDirectory(position);
            notifyDataSetChanged();
        } else {
            // User pressed a file so select/deselect it
            String filePath = file.getAbsolutePath();
            if (!mSelectedFilePaths.contains(filePath)) {
                mSelectedFilePaths.add(file.getAbsolutePath());
                // Notify listener
                if (mFileSelectionListener != null) {
                    // Create GeneralFile object
                    File selectedFile = mFileManager.getFileAtIndex(position);
                    mFileSelectionListener.onFileSelected(new GeneralFile(
                            selectedFile,
                            Utilities.drawableToBitmap(mContext.getDrawable(R.drawable.ic_file))
                    ));
                }
                if (mThumbnailAnimate != null) {
                    ImageView thumbnail = itemView.findViewById(R.id.iv_icon_item);
                    mThumbnailAnimate.thumbnailAnimate(thumbnail);
                }
            } else {
                mSelectedFilePaths.remove(filePath);
                // Notify listener
                if (mFileSelectionListener != null) {
                    // Create GeneralFile object
                    File selectedFile = mFileManager.getFileAtIndex(position);
                    mFileSelectionListener.onFileDeselected(new GeneralFile(
                            selectedFile,
                            Utilities.drawableToBitmap(mContext.getDrawable(R.drawable.ic_file))
                    ));
                }
            }
            notifyItemChanged(position);
        }
    }

    private void toggleDirectorySelection(int position, FileSystemEntryViewHolder viewHolder, View itemView) {
        File file = mFileManager.getFileAtIndex(position);
        String dirPath = file.getAbsolutePath();
        if (!mSelectedFilePaths.contains(dirPath)) {
            mSelectedFilePaths.add(dirPath);
            // Notify listener
            if (mFileSelectionListener != null) {
                // Create GeneralFile object
                mFileSelectionListener.onFileSelected(new GeneralFile(
                        file,
                        Utilities.drawableToBitmap(mContext.getDrawable(R.drawable.ic_folder_closed)),
                        mFileManager.getChildCountAtIndex(position)
                ));
            }
            if (mThumbnailAnimate != null) {
                ImageView thumbnail = itemView.findViewById(R.id.iv_icon_item);
                mThumbnailAnimate.thumbnailAnimate(thumbnail);
            }
        } else {
            mSelectedFilePaths.remove(dirPath);
            // Notify listener
            if (mFileSelectionListener != null) {
                // Create GeneralFile object
                mFileSelectionListener.onFileDeselected(new GeneralFile(
                        file,
                        Utilities.drawableToBitmap(mContext.getDrawable(R.drawable.ic_folder_closed)),
                        mFileManager.getChildCountAtIndex(position)
                ));
            }
        }
        notifyItemChanged(position);
    }

    public void deselectFile(GeneralFile selectedFile) {
        mSelectedFilePaths.remove(selectedFile.getUid());
        notifyDataSetChanged();
    }
}
