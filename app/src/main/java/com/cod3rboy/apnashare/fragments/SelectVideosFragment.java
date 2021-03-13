package com.cod3rboy.apnashare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.cod3rboy.apnashare.R;

import java.util.ArrayList;

import com.cod3rboy.apnashare.activities.FilesSelectionActivity;
import com.cod3rboy.apnashare.adapters.VideosAdapter;
import com.cod3rboy.apnashare.background.VideosAdapterDataLoader;
import com.cod3rboy.apnashare.models.BasicFile;
import com.cod3rboy.apnashare.interfaces.OnFileSelectionListener;
import com.cod3rboy.apnashare.managers.ThumbnailManager;
import com.cod3rboy.apnashare.models.VideoFile;

public class SelectVideosFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<ArrayList<VideoFile>>, OnFileSelectionListener {

    private RecyclerView mVideosRecyclerView;
    private ProgressBar mLoadingView;
    private TextView mEmptyView;
    private VideosAdapter mAdapter;
    private LoaderManager mLoaderMgr;
    private ThumbnailManager mThumbMgr;
    private static final int VIDEOS_LOADER_ID = 104;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_videos, container, false);
        mLoadingView = view.findViewById(R.id.loading_view);
        mEmptyView = view.findViewById(R.id.empty_view);
        mVideosRecyclerView = view.findViewById(R.id.rv_videos);
        mVideosRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        mVideosRecyclerView.setHasFixedSize(true);
        ((SimpleItemAnimator) mVideosRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        mThumbMgr = new ThumbnailManager(getContext(), ThumbnailManager.ThumbnailType.THUMBNAIL_TYPE_VIDEO);

        mAdapter = new VideosAdapter(getContext(), mThumbMgr);
        mAdapter.setVideoSelectionListener(this);
        mVideosRecyclerView.setAdapter(mAdapter);

        mVideosRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                mAdapter.setRecyclerViewState(newState);
            }
        });

        mAdapter.setThumbnailAnimate(originalThumbnail -> ((FilesSelectionActivity) getActivity()).translateThumbnailToSelectedButton(originalThumbnail));

        // Start loader to load videos data in com.cod3rboy.apnashare.background
        mLoaderMgr = LoaderManager.getInstance(this);
        mLoaderMgr.initLoader(VIDEOS_LOADER_ID, null, this);
        return view;
    }

    public void deselectVideo(VideoFile selectedVideo) {
        mAdapter.deselectVideo(selectedVideo);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbMgr.dispose();
    }

    @NonNull
    @Override
    public Loader<ArrayList<VideoFile>> onCreateLoader(int id, @Nullable Bundle args) {
        return new VideosAdapterDataLoader(getContext());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<ArrayList<VideoFile>> loader, ArrayList<VideoFile> data) {
        mLoadingView.setVisibility(View.GONE);
        if (data.size() > 0) {
            mVideosRecyclerView.setVisibility(View.VISIBLE);
            mAdapter.setData(data);
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
        }
        mLoaderMgr.destroyLoader(VIDEOS_LOADER_ID);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<ArrayList<VideoFile>> loader) {
    }

    @Override
    public void onFileSelected(BasicFile selectedFile) {
        ((FilesSelectionActivity) getActivity()).fileSelected(selectedFile);
    }

    @Override
    public void onFileDeselected(BasicFile selectedFile) {
        ((FilesSelectionActivity) getActivity()).fileDeselected(selectedFile);
    }
}
