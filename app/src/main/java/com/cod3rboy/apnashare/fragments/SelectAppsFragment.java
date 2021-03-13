package com.cod3rboy.apnashare.fragments;

import android.os.Bundle;
import android.util.Log;
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

import com.cod3rboy.apnashare.BuildConfig;
import com.cod3rboy.apnashare.R;

import java.util.ArrayList;

import com.cod3rboy.apnashare.activities.FilesSelectionActivity;
import com.cod3rboy.apnashare.adapters.AppsAdapter;
import com.cod3rboy.apnashare.background.AppsAdapterDataLoader;
import com.cod3rboy.apnashare.models.BasicFile;
import com.cod3rboy.apnashare.interfaces.OnFileSelectionListener;
import com.cod3rboy.apnashare.managers.ThumbnailManager;
import com.cod3rboy.apnashare.models.AppFile;

public class SelectAppsFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<ArrayList<AppFile>>, OnFileSelectionListener {

    private static final String LOG_TAG = SelectAppsFragment.class.getSimpleName();

    private RecyclerView mAppsRecyclerView;
    private ProgressBar mLoadingView;
    private TextView mEmptyView;
    private LoaderManager mLoaderMgr;
    private AppsAdapter mAdapter;
    private ThumbnailManager mThumbMgr;

    private static final int APPS_LOADER_ID = 102;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "onCreateView() called");
        View v = inflater.inflate(R.layout.fragment_select_apps, container, false);
        mLoadingView = v.findViewById(R.id.loading_view);
        mEmptyView = v.findViewById(R.id.empty_view);
        mAppsRecyclerView = v.findViewById(R.id.rv_apps);
        mAppsRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 4));
        mAppsRecyclerView.setHasFixedSize(true);
        ((SimpleItemAnimator) mAppsRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        mThumbMgr = new ThumbnailManager(getContext(), ThumbnailManager.ThumbnailType.THUMBNAIL_TYPE_APP);
        mAdapter = new AppsAdapter(getContext(), mThumbMgr);
        mAdapter.setAppSelectionListener(this);
        mAppsRecyclerView.setAdapter(mAdapter);
        mAppsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                mAdapter.setRecyclerViewState(newState);
            }
        });
        mAdapter.setThumbnailAnimate(originalThumbnail -> ((FilesSelectionActivity) getActivity()).translateThumbnailToSelectedButton(originalThumbnail));

        // Start loader to load apps data in background
        mLoaderMgr = LoaderManager.getInstance(this);
        mLoaderMgr.initLoader(APPS_LOADER_ID, null, this);
        return v;
    }

    public void deselectApp(AppFile appInfo) {
        mAdapter.deselectApp(appInfo);
    }

    @Override
    public void onDestroyView() {
        if (BuildConfig.DEBUG) Log.d(LOG_TAG, "onDestroyView() called");
        super.onDestroyView();
        mThumbMgr.dispose();
    }

    @NonNull
    @Override
    public Loader<ArrayList<AppFile>> onCreateLoader(int id, @Nullable Bundle args) {
        return new AppsAdapterDataLoader(getContext());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<ArrayList<AppFile>> loader, ArrayList<AppFile> data) {
        mLoadingView.setVisibility(View.GONE);
        if (data.size() > 0) {
            mAppsRecyclerView.setVisibility(View.VISIBLE);
            mAdapter.setData(data);
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
        }
        mLoaderMgr.destroyLoader(APPS_LOADER_ID);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<ArrayList<AppFile>> loader) {
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
