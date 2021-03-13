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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.cod3rboy.apnashare.R;

import java.util.ArrayList;

import com.cod3rboy.apnashare.activities.FilesSelectionActivity;
import com.cod3rboy.apnashare.adapters.MusicAdapter;
import com.cod3rboy.apnashare.background.MusicAdapterDataLoader;
import com.cod3rboy.apnashare.models.BasicFile;
import com.cod3rboy.apnashare.interfaces.OnFileSelectionListener;
import com.cod3rboy.apnashare.managers.ThumbnailManager;
import com.cod3rboy.apnashare.models.AudioFile;

public class SelectMusicFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<ArrayList<AudioFile>>, OnFileSelectionListener {
    private RecyclerView mMusicRecyclerView;
    private ProgressBar mLoadingView;
    private TextView mEmptyView;
    private LoaderManager mLoaderMgr;
    private MusicAdapter mAdapter;
    private ThumbnailManager mThumbMgr;

    private static final int MUSIC_LOADER_ID = 103;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_music, container, false);
        mLoadingView = view.findViewById(R.id.loading_view);
        mEmptyView = view.findViewById(R.id.empty_view);
        mMusicRecyclerView = view.findViewById(R.id.rv_music);
        mMusicRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mMusicRecyclerView.setHasFixedSize(true);
        ((SimpleItemAnimator) mMusicRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        mThumbMgr = new ThumbnailManager(getContext(), ThumbnailManager.ThumbnailType.THUMBNAIL_TYPE_MUSIC);

        mAdapter = new MusicAdapter(getContext(), mThumbMgr);
        mAdapter.setMusicSelectionListener(this);
        mMusicRecyclerView.setAdapter(mAdapter);
        mMusicRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                mAdapter.setRecyclerViewState(newState);
            }
        });

        mAdapter.setThumbnailAnimate(originalThumbnail -> ((FilesSelectionActivity) getActivity()).translateThumbnailToSelectedButton(originalThumbnail));

        // Start loader to load Music data
        mLoaderMgr = LoaderManager.getInstance(this);
        mLoaderMgr.initLoader(MUSIC_LOADER_ID, null, this);
        return view;
    }

    public void deselectMusic(AudioFile selectedMusic) {
        mAdapter.deselectMusic(selectedMusic);
    }

    @NonNull
    @Override
    public Loader<ArrayList<AudioFile>> onCreateLoader(int id, @Nullable Bundle args) {
        return new MusicAdapterDataLoader(getContext());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<ArrayList<AudioFile>> loader, ArrayList<AudioFile> data) {
        mLoadingView.setVisibility(View.GONE);
        if (data.size() > 0) {
            mMusicRecyclerView.setVisibility(View.VISIBLE);
            mAdapter.setData(data);
        } else {
            mEmptyView.setVisibility(View.VISIBLE);
        }
        mLoaderMgr.destroyLoader(MUSIC_LOADER_ID);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<ArrayList<AudioFile>> loader) {
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
