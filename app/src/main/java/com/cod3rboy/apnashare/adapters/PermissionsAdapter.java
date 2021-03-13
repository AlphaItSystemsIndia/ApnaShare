package com.cod3rboy.apnashare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.models.Permission;
import com.cod3rboy.apnashare.viewholders.PermissionViewHolder;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class PermissionsAdapter extends RecyclerView.Adapter<PermissionViewHolder> {

    private Context mContext;
    private ArrayList<Permission> mPermissionModels;

    public PermissionsAdapter(Context context) {
        mContext = context;
        mPermissionModels = new ArrayList<>();
    }

    public void setPermissionModels(ArrayList<Permission> models) {
        mPermissionModels.clear();
        mPermissionModels.addAll(models);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PermissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_permission, parent, false);
        PermissionViewHolder holder = new PermissionViewHolder(itemView);
        MaterialButton actionButton = itemView.findViewById(R.id.btn_action);
        actionButton.setOnClickListener((view) -> {
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) return;
            Permission permission = mPermissionModels.get(position);
            permission.executeAction();
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull PermissionViewHolder holder, int position) {
        holder.setData(mPermissionModels.get(position));
    }

    @Override
    public int getItemCount() {
        return mPermissionModels.size();
    }

    public boolean allPermissionsGranted() {
        for (Permission p : mPermissionModels)
            if (!p.isGranted()) return false;
        return true;
    }

    public void setPermissionGranted(Permission.Type type, boolean value) {
        for (int i = 0; i < mPermissionModels.size(); i++) {
            Permission p = mPermissionModels.get(i);
            if (p.getType() == type) {
                p.setGranted(value);
                notifyItemChanged(i);
                break;
            }
        }
    }
}
