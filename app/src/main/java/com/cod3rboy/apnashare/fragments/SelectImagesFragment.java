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
import com.cod3rboy.apnashare.adapters.ImagesAdapter;
import com.cod3rboy.apnashare.background.ImagesAdapterDataLoader;
import com.cod3rboy.apnashare.models.BasicFile;
import com.cod3rboy.apnashare.interfaces.OnFileSelectionListener;
import com.cod3rboy.apnashare.managers.ThumbnailManager;
import com.cod3rboy.apnashare.models.ImageFile;

public class SelectImagesFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<ArrayList<ImageFile>>, OnFileSelectionListener {
    private RecyclerView mImagesRecyclerView;
    private ProgressBar mLoadingView;
    private TextView mEmptyView;
    private LoaderManager mLoaderMgr;
    private ImagesAdapter mAdapter;
    private ThumbnailManager mThumbMgr;

    private static final int IMAGES_LOADER_ID = 101;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_images, container, false);
        mLoadingView = view.findViewById(R.id.loading_view);
        mEmptyView = view.findViewById(R.id.empty_view);
        mImagesRecyclerView = view.findViewById(R.id.rv_images);
        mImagesRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        mImagesRecyclerView.setHasFixedSize(true);
        ((SimpleItemAnimator) mImagesRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        mThumbMgr = new ThumbnailManager(getContext(), ThumbnailManager.ThumbnailType.THUMBNAIL_TYPE_IMAGE);

        mAdapter = new ImagesAdapter(getContext(), mThumbMgr);
        mAdapter.setImageSelectionListener(this);
        mImagesRecyclerView.setAdapter(mAdapter);

        // Start loader to load data and set adapter on recycler view
        mLoaderMgr = LoaderManager.getInstance(this);
        mLoaderMgr.initLoader(IMAGES_LOADER_ID, null, this);

        // Register scroll listener on RecyclerView
        mImagesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                mAdapter.setRecyclerViewState(newState);
            }
        });

        mAdapter.setThumbnailAnimate(originalThumbnail -> ((FilesSelectionActivity) getActivity()).translateThumbnailToSelectedButton(originalThumbnail));

        return view;
    }

    public void deselectImage(ImageFile selectedImage) {
        mAdapter.deselectImage(selectedImage);
    }

    @NonNull
    @Override
    public Loader<ArrayList<ImageFile>> onCreateLoader(int id, @Nullable Bundle args) {
        return new ImagesAdapterDataLoader(getContext());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<ArrayList<ImageFile>> loader, ArrayList<ImageFile> data) {
        mLoadingView.setVisibility(View.GONE);
        if (data.size() > 0) {
            mImagesRecyclerView.setVisibility(View.VISIBLE);
            mAdapter.setData(data);
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
        }
        mLoaderMgr.destroyLoader(IMAGES_LOADER_ID);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<ArrayList<ImageFile>> loader) {
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbMgr.dispose();
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
