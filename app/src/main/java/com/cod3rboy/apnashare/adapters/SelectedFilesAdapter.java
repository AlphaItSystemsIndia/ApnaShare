package com.cod3rboy.apnashare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.misc.FileSelectionQueue;
import com.cod3rboy.apnashare.models.BasicFile;
import com.cod3rboy.apnashare.viewholders.SelectedFileViewHolder;

public class SelectedFilesAdapter extends RecyclerView.Adapter<SelectedFileViewHolder> {

    public interface IMediator {
        void requestFileDeselection(BasicFile file);

        void deselectionSuccessful();
    }

    private Context mContext;
    private FileSelectionQueue mQueue;
    private IMediator mMediator;

    public SelectedFilesAdapter(@NonNull Context context, @NonNull IMediator mediator) {
        mContext = context;
        mQueue = FileSelectionQueue.getInstance();
        mQueue.initialize();
        mMediator = mediator;
    }

    public void addSelectedFile(BasicFile selectedFile) {
        int pos = mQueue.add(selectedFile);
        notifyItemInserted(pos);
    }

    public void removeSelectedFile(BasicFile deselectedFile) {
        int pos = mQueue.remove(deselectedFile);
        if (pos >= 0) notifyItemRemoved(pos);
    }

    public void clearAll() {
        for (BasicFile file : mQueue.getAll()) {
            mMediator.requestFileDeselection(file);
        }
        mQueue.clearAll();
        notifyDataSetChanged();
        mMediator.deselectionSuccessful();
    }

    @NonNull
    @Override
    public SelectedFileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_selected_file, parent, false);
        SelectedFileViewHolder holder = new SelectedFileViewHolder(itemView);
        ImageButton clearFileBtn = itemView.findViewById(R.id.btn_clear_file);
        clearFileBtn.setOnClickListener((view) -> {
            int itemPosition = holder.getAdapterPosition();
            if (itemPosition != RecyclerView.NO_POSITION) {
                // request file deselection
                mMediator.requestFileDeselection(mQueue.getAtPosition(itemPosition));
                mQueue.removeAtPosition(itemPosition);
                notifyItemRemoved(itemPosition);
                mMediator.deselectionSuccessful();
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull SelectedFileViewHolder holder, int position) {
        holder.setData(mQueue.getAtPosition(position));
    }

    @Override
    public int getItemCount() {
        return mQueue.count();
    }
}
