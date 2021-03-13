package com.cod3rboy.apnashare.viewholders;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;

public class ReceiverViewHolder extends RecyclerView.ViewHolder {
    private TextView mReceiverNameView;

    public ReceiverViewHolder(@NonNull View itemView) {
        super(itemView);
        mReceiverNameView = itemView.findViewById(R.id.tv_receiver_name);
    }

    public void setReceiverName(String name) {
        mReceiverNameView.setText(name);
    }
}
