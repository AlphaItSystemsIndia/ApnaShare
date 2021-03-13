package com.cod3rboy.apnashare.activities;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.adapters.IntentFilesAdapter;
import com.cod3rboy.apnashare.background.LoadFilesFromIntentTask;
import com.cod3rboy.apnashare.misc.FileSelectionQueue;
import com.cod3rboy.apnashare.models.BasicFile;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;

import needle.Needle;

public class ActionSendActivity extends AppCompatActivity {

    private MaterialButton mSendButton;
    private MaterialButton mCancelButton;
    private RecyclerView mFilesRecyclerView;
    private IntentFilesAdapter mAdapter;
    private BottomSheetBehavior<View> mPopupSheet;
    private boolean hasExpanded;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            if (action != null && (action.equals(Intent.ACTION_SEND) || action.equals(Intent.ACTION_SEND_MULTIPLE))) {
                initializeView();
                handleIntent(intent, action);
            }
        }
    }

    private void initializeView() {
        setContentView(R.layout.activity_action_send);

        mSendButton = findViewById(R.id.btn_send);
        mCancelButton = findViewById(R.id.btn_cancel);
        mFilesRecyclerView = findViewById(R.id.rv_files);

        View popUpView = findViewById(R.id.bs_send_action);
        mPopupSheet = BottomSheetBehavior.from(popUpView);
        mPopupSheet.setState(BottomSheetBehavior.STATE_HIDDEN);

        popUpView.postDelayed(() -> {
            // In order to display bottom sheet with animation, it is displayed after some delay.
            mPopupSheet.setState(BottomSheetBehavior.STATE_EXPANDED);
            hasExpanded = true;
        }, 200);

        mPopupSheet.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (hasExpanded && newState == BottomSheetBehavior.STATE_HIDDEN)
                    finish(); // Finish activity if bottom sheet was dismissed
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Do nothing here
            }
        });

        hasExpanded = false;

        mFilesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mFilesRecyclerView.setHasFixedSize(true);
        mAdapter = new IntentFilesAdapter(this);
        mFilesRecyclerView.setAdapter(mAdapter);

        mCancelButton.setOnClickListener(v -> finish());

        mSendButton.setOnClickListener(v -> startSendActivityAndFinishMe());
    }

    private void handleIntent(@NonNull Intent intent, @NonNull String intentAction) {
        FileSelectionQueue queueToFill = FileSelectionQueue.getInstance();
        ContentResolver resolver = getContentResolver();
        queueToFill.initialize();
        // Load files to send
        LoadFilesFromIntentTask loadFilesTask = new LoadFilesFromIntentTask(
                resolver,
                intent,
                intentAction,
                queueToFill,
                () -> {
                    mAdapter.notifyDataSetChanged();
                    if (!queueToFill.isEmpty()) mSendButton.setEnabled(true);
                }
        );
        Needle.onBackgroundThread().execute(loadFilesTask);
    }

    private void startSendActivityAndFinishMe() {
        FileSelectionQueue filledQueue = FileSelectionQueue.getInstance();
        Intent intent = new Intent(this, SendActivity.class);
        // Forwarding grant read uri permission to send activity
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ClipData clipData = null;
        for (int i = 0; i < filledQueue.count(); i++) {
            BasicFile selectedFile = filledQueue.getAtPosition(i);
            if (!selectedFile.getPath().startsWith(ContentResolver.SCHEME_CONTENT)) continue;
            Uri fileUri = Uri.parse(selectedFile.getPath());
            if (clipData == null)
                clipData = ClipData.newUri(getContentResolver(), null, fileUri);
            else
                clipData.addItem(new ClipData.Item(fileUri));
        }
        intent.setClipData(clipData);
        startActivity(intent);
        finish();
    }
}