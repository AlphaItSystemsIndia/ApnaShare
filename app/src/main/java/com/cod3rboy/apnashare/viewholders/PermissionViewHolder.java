package com.cod3rboy.apnashare.viewholders;

import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.models.Permission;

public class PermissionViewHolder extends RecyclerView.ViewHolder {
    private TextView mNameTextView;
    private TextView mDescTextView;
    private Button mActionBtn;
    private ImageView mCheckView;
    private ImageView mIconView;

    public PermissionViewHolder(@NonNull View itemView) {
        super(itemView);
        mNameTextView = itemView.findViewById(R.id.tv_permission_name);
        mDescTextView = itemView.findViewById(R.id.tv_permission_desc);
        mActionBtn = itemView.findViewById(R.id.btn_action);
        mCheckView = itemView.findViewById(R.id.iv_checked);
        mIconView = itemView.findViewById(R.id.iv_permission_icon);
    }

    public void setData(Permission permissionModel) {
        mNameTextView.setText(permissionModel.getName());
        mDescTextView.setText(permissionModel.getDescription());
        mActionBtn.setText(permissionModel.getActionName());
        mIconView.setImageDrawable(permissionModel.getIcon());
        if (permissionModel.isGranted()) {
            mActionBtn.setVisibility(View.GONE);
            mCheckView.setVisibility(View.VISIBLE);
        } else {
            mActionBtn.setVisibility(View.VISIBLE);
            mCheckView.setVisibility(View.GONE);
        }
    }
}
