package com.cod3rboy.apnashare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.viewholders.ReceiverViewHolder;

import java.util.ArrayList;

public class ReceiversAdapter extends RecyclerView.Adapter<ReceiverViewHolder> {

    public interface OnItemClickListener {
        void itemClicked(int position);
    }

    private Context mContext;
    private ArrayList<String> mReceivers;
    private String mConnectedReceiver;
    private OnItemClickListener mListener;

    public ReceiversAdapter(Context context) {
        mContext = context;
        mReceivers = new ArrayList<>();
        mConnectedReceiver = null;
    }

    public void registerItemClickListener(OnItemClickListener listener) {
        mListener = listener;
    }

    public void unregisterItemClickListener() {
        mListener = null;
    }

    public void addReceiver(String receiver) {
        mReceivers.add(receiver);
        notifyItemInserted(mReceivers.size() - 1);
    }

    public void removeReceiver(String receiver) {
        int pos = mReceivers.indexOf(receiver);
        if (pos >= 0) {
            mReceivers.remove(pos);
            notifyItemRemoved(pos);
        }
    }

    public void setConnectedReceiver(String receiver) {
        mConnectedReceiver = receiver;
        notifyDataSetChanged();
    }

    public String getReceiverAtPosition(int position) {
        return mReceivers.get(position);
    }

    public String getConnectedReceiver() {
        return mConnectedReceiver;
    }

    public void clearAll() {
        mReceivers.clear();
        mConnectedReceiver = null;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReceiverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_receiver, parent, false);
        ReceiverViewHolder holder = new ReceiverViewHolder(itemView);
        itemView.setOnClickListener((view) -> {
            int position = holder.getAdapterPosition();
            if (position != RecyclerView.NO_POSITION && mListener != null) {
                mListener.itemClicked(position);
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ReceiverViewHolder holder, int position) {
        String receiver = mReceivers.get(position);
        holder.setReceiverName(receiver);
    }

    @Override
    public int getItemCount() {
        return mReceivers.size();
    }

}
