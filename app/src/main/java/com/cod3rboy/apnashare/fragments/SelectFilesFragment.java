package com.cod3rboy.apnashare.fragments;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.activities.FilesSelectionActivity;
import com.cod3rboy.apnashare.adapters.FileSystemAdapter;
import com.cod3rboy.apnashare.models.BasicFile;
import com.cod3rboy.apnashare.interfaces.OnFileSelectionListener;
import com.cod3rboy.apnashare.managers.FileManager;
import com.cod3rboy.apnashare.models.GeneralFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

public class SelectFilesFragment extends Fragment
        implements FileManager.NavigationListener, OnFileSelectionListener {
    private static final String LOG_TAG = SelectFilesFragment.class.getSimpleName();
    private View mPrimaryLayout;
    private View mSecondaryLayout;
    private RecyclerView mRootsRecyclerView;
    private RecyclerView mFileSystemRecyclerView;
    private FileManager mFileManager;
    private FileSystemAdapter mAdapter;

    private Stack<Integer> mSavedScrollPositions;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_files, container, false);
        mPrimaryLayout = view.findViewById(R.id.files_layout_primary);
        mSecondaryLayout = view.findViewById(R.id.files_layout_secondary);
        mRootsRecyclerView = view.findViewById(R.id.rv_root_dirs);
        mFileSystemRecyclerView = view.findViewById(R.id.rv_file_system);
        mFileSystemRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mFileSystemRecyclerView.setHasFixedSize(true);
        ((SimpleItemAnimator) mFileSystemRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        mFileManager = new FileManager(getContext());
        mFileManager.addNavigationListener(this);

        mRootsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRootsRecyclerView.setHasFixedSize(true);
        RootItemAdapter rootDirsAdapter = new RootItemAdapter(getContext());
        rootDirsAdapter.setRootDirectories(mFileManager.getAvailableExternalRoots());
        mRootsRecyclerView.setAdapter(rootDirsAdapter);
        DividerItemDecoration dividerDecoration = new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL);
        dividerDecoration.setDrawable(getResources().getDrawable(R.drawable.list_divider));
        mRootsRecyclerView.addItemDecoration(dividerDecoration);

        rootDirsAdapter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Hide Primary Layout and Show Secondary Layout
                // Also create and set adapter on the RecyclerView
                mFileManager.visitDirectory(position);
                mPrimaryLayout.setVisibility(View.GONE);
                mSecondaryLayout.setVisibility(View.VISIBLE);
                mAdapter.notifyDataSetChanged();
            }
        });

        mAdapter = new FileSystemAdapter(getContext(), mFileManager);
        mAdapter.setFileSelectionListener(this);
        mFileSystemRecyclerView.setAdapter(mAdapter);

        mAdapter.setThumbnailAnimate(originalThumbnail -> ((FilesSelectionActivity) getActivity()).translateThumbnailToSelectedButton(originalThumbnail));

        mSavedScrollPositions = new Stack<>();
        return view;
    }

    public void deselectFile(GeneralFile selectedFile) {
        mAdapter.deselectFile(selectedFile);
    }

    public void onBackPressed() {
        if (mFileManager.isRootActiveDirectory()) {
            // Root is active so back press kills host activity
            getActivity().finish();
        } else {
            // Inside file system so back press moves to parent directory
            mFileManager.visitDirectory(0); // Directory at Position-0 always refer to parent directory
            // Notify data set for recyclerview has changed
            FileSystemAdapter adapter = (FileSystemAdapter) mFileSystemRecyclerView.getAdapter();
            if (adapter != null) adapter.notifyDataSetChanged();
        }
    }


    @Override
    public void backToRoot() {
        // Show Primary Layout and Hide Secondary Layout
        mSecondaryLayout.setVisibility(View.GONE);
        mPrimaryLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void subdirectoryVisited(int position) {
        //@todo fix last position restoration
        // Get position of last item visible of screen
        int lastVisibleItemPosition = ((LinearLayoutManager) mFileSystemRecyclerView.getLayoutManager()).findLastVisibleItemPosition();
        // Save last item position
        mSavedScrollPositions.push(lastVisibleItemPosition);
        mFileSystemRecyclerView.scrollToPosition(0); // scroll list to top.
    }

    @Override
    public void backToParent() {
        // Load position of last item
        int lastVisibleItemPosition = mSavedScrollPositions.pop();
        Log.d(LOG_TAG, "Restore parent visible item at position: " + lastVisibleItemPosition);
        mFileSystemRecyclerView.postDelayed(() -> mFileSystemRecyclerView.scrollToPosition(lastVisibleItemPosition), 350);
    }

    @Override
    public void forwardChildrenLoaded() {
        mAdapter.notifyDataSetChanged();
    }


    @Override
    public void onFileSelected(BasicFile selectedFile) {
        ((FilesSelectionActivity) getActivity()).fileSelected(selectedFile);
    }

    @Override
    public void onFileDeselected(BasicFile selectedFile) {
        ((FilesSelectionActivity) getActivity()).fileDeselected(selectedFile);
    }

    class RootItemAdapter extends RecyclerView.Adapter<RootItemViewHolder> {
        private Context mContext;
        private AdapterView.OnItemClickListener mItemClickListener;
        private ArrayList<String> mDrives;

        public RootItemAdapter(Context context) {
            mContext = context;
            mDrives = new ArrayList<>();
            mItemClickListener = null;
        }

        @NonNull
        @Override
        public RootItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(mContext).inflate(R.layout.item_drive, parent, false);
            RootItemViewHolder holder = new RootItemViewHolder(itemView);
            itemView.setOnClickListener((v) -> {
                int position = holder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    if (mItemClickListener != null)
                        mItemClickListener.onItemClick(null, v, position, -1);
                }
            });
            return holder;
        }

        public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
            mItemClickListener = listener;
        }

        @Override
        public void onBindViewHolder(@NonNull RootItemViewHolder holder, int position) {
            String driveName = mDrives.get(position);
            int resIcon = (driveName.equals(getString(R.string.drive_name_internal))) ? R.drawable.ic_drive : R.drawable.ic_sdcard;
            holder.setData(driveName, resIcon);
        }

        public void setRootDirectories(ArrayList<String> drives) {
            mDrives.clear();
            for (String drive : drives) {
                if (Environment.isExternalStorageEmulated(new File(drive))) {
                    mDrives.add(getString(R.string.drive_name_internal));
                } else {
                    mDrives.add(getString(R.string.drive_name_external));
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return mDrives.size();
        }
    }

    class RootItemViewHolder extends RecyclerView.ViewHolder {
        private TextView mRootName;
        private ImageView mRootIcon;

        public RootItemViewHolder(@NonNull View itemView) {
            super(itemView);
            mRootName = itemView.findViewById(R.id.tv_root_name);
            mRootIcon = itemView.findViewById(R.id.iv_drive_icon);
        }

        public void setData(String rootName, int resRootIcon) {
            mRootName.setText(rootName);
            mRootIcon.setImageResource(resRootIcon);
        }
    }
}
