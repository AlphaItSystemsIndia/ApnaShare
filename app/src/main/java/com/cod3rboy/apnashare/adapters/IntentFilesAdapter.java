package com.cod3rboy.apnashare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.misc.FileSelectionQueue;
import com.cod3rboy.apnashare.viewholders.SelectedFileViewHolder;

public class IntentFilesAdapter extends RecyclerView.Adapter<SelectedFileViewHolder> {
    private Context context;
    private FileSelectionQueue filesQueue;

    public IntentFilesAdapter(Context context) {
        this.context = context;
        this.filesQueue = FileSelectionQueue.getInstance();
    }

    @NonNull
    @Override
    public SelectedFileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.item_action_send_file, parent, false);
        return new SelectedFileViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectedFileViewHolder holder, int position) {
        holder.setData(filesQueue.getAtPosition(position));
    }

    @Override
    public int getItemCount() {
        return filesQueue.count();
    }
}