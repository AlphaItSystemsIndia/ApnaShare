package com.cod3rboy.apnashare.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.activities.SendActivity;
import com.cod3rboy.apnashare.adapters.ProgressFilesAdapter;
import com.cod3rboy.apnashare.misc.Utilities;
import com.cod3rboy.apnashare.models.TransmissionFile;
import com.cod3rboy.apnashare.transmission.events.ProtocolMismatch;
import com.cod3rboy.apnashare.transmission.events.ProgressUpdate;
import com.cod3rboy.apnashare.transmission.events.client.TUnitCompleted;
import com.cod3rboy.apnashare.transmission.events.client.TUnitStarted;
import com.cod3rboy.apnashare.transmission.events.client.TransmissionEnded;
import com.cod3rboy.apnashare.transmission.events.client.TransmissionStarted;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class ProgressFilesFragment extends Fragment {

    private RecyclerView mProgressFilesRecyclerView;
    private ProgressFilesAdapter mProgressFilesAdapter;
    private LottieAnimationView mRocketView;
    private LottieAnimationView mCloudView;
    private TextView mStatusView;
    private View mProgressView;
    private TextView mSentData;
    private TextView mSentDataUnit;
    private String mReceiverSSID;
    private TextView mReceiverIdView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_progress_files, container, false);
        mReceiverSSID = "";
        mStatusView = v.findViewById(R.id.status);
        mReceiverIdView = v.findViewById(R.id.tv_receiver_name);
        mProgressFilesRecyclerView = v.findViewById(R.id.rv_progress_files);
        mRocketView = v.findViewById(R.id.rocket_view);
        mCloudView = v.findViewById(R.id.cloud_view);
        mProgressView = v.findViewById(R.id.progress_view);
        mSentData = mProgressView.findViewById(R.id.tv_size_total);
        mSentDataUnit = mProgressView.findViewById(R.id.tv_size_unit);
        mProgressFilesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mProgressFilesRecyclerView.setHasFixedSize(true);
        ((SimpleItemAnimator) mProgressFilesRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        mProgressFilesAdapter = new ProgressFilesAdapter(getContext());
        mProgressFilesRecyclerView.setAdapter(mProgressFilesAdapter);
        ImageButton closeBtn = v.findViewById(R.id.btn_close);
        closeBtn.setOnClickListener(view -> ((SendActivity) getActivity()).exitSender());
        return v;
    }

    public void setReceiverSSID(String ssid) {
        mReceiverSSID = ssid;
        mStatusView.setText(getString(R.string.status_connecting_send_progress, mReceiverSSID));
    }

    private void launchRocket() {
        mRocketView.setAnimation(R.raw.rocket_launched);
        mRocketView.setRepeatCount(LottieDrawable.INFINITE);
        mRocketView.setRepeatMode(LottieDrawable.RESTART);
        mRocketView.playAnimation();
        mRocketView.animate()
                .setDuration(800)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .translationYBy(-(mRocketView.getTop() - mStatusView.getBottom() - 100))
                .alpha(0)
                .withEndAction(() -> {
                    // Hide Rocket
                    mRocketView.cancelAnimation();
                    mRocketView.setVisibility(View.GONE);
                }).start();

        mCloudView.animate()
                .setDuration(400)
                .setInterpolator(new AccelerateInterpolator())
                .translationY(mCloudView.getHeight())
                .alpha(0)
                .withEndAction(() -> {
                    // Hide Cloud
                    mCloudView.cancelAnimation();
                    mCloudView.setVisibility(View.GONE);
                }).start();

        mProgressView.setVisibility(View.VISIBLE);
        mProgressView.setAlpha(0);
        mProgressView.animate()
                .setDuration(400)
                .setStartDelay(400)
                .setInterpolator(new AccelerateInterpolator())
                .alpha(1)
                .withEndAction(() -> {
                    // Make Progress Files Visible
                    mProgressView.setAlpha(1);
                }).start();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mProgressFilesAdapter.setFilesInProgress(((SendActivity) getActivity()).getFilesToSend());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void globalProgressUpdate(long totalBytesTransmitted) {
        // Update Bytes Received View
        String[] totalDataSent = Utilities.convertBytesToString(totalBytesTransmitted).split("\\s+");
        mSentData.setText(totalDataSent[0]);
        mSentDataUnit.setText(totalDataSent[1]);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTransmissionStarted(TransmissionStarted event) {
        mStatusView.setText(R.string.status_sending_files);
        mReceiverIdView.setText(getString(R.string.status_receiver_connected, mReceiverSSID));
        launchRocket();
        globalProgressUpdate(0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTransmissionEnded(TransmissionEnded event) {
        if (!event.wasSuccessful()) {
            mStatusView.setText(R.string.status_send_failed);
            mReceiverIdView.setText(R.string.status_receiver_disconnected);
            mProgressFilesAdapter.markAllPendingFilesAsFailed();
        } else {
            mStatusView.setText(R.string.status_send_successful);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTUnitStarted(TUnitStarted event) {
        mProgressFilesAdapter.changeFileState(event.getFile(), TransmissionFile.State.PROGRESSING);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTUnitCompleted(TUnitCompleted event) {
        mProgressFilesAdapter.changeFileState(event.getFile(), TransmissionFile.State.SUCCESS);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onProgressUpdate(ProgressUpdate event) {
        globalProgressUpdate(event.getTotalBytesProcessed());
        mProgressFilesAdapter.updateProgressFile(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onProtocolMismatch(ProtocolMismatch event) {
        // Show Protocol mismatched alert dialog
        int lv = event.getLocalProtocolVersion(); // Local Version
        int rv = event.getRemoteProtocolVersion(); // Remote Version
        String msgForOldVersion = getString(R.string.protocol_mismatch_dialog_msg_for_old);
        String msgForNewVersion = getString(R.string.protocol_mismatch_dialog_msg_for_new);
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(getContext());
        dialogBuilder.setTitle(R.string.protocol_mismatch_dialog_title);
        dialogBuilder.setOnDismissListener(dialog -> ((SendActivity) getActivity()).exitSender());
        if (lv > rv) {
            dialogBuilder.setMessage(msgForNewVersion);
            dialogBuilder.setPositiveButton(R.string.protocol_mismatch_dialog_ok_btn, (dialog, which) -> dialog.dismiss());
        } else if (rv > lv) {
            dialogBuilder.setMessage(msgForOldVersion);
            dialogBuilder.setPositiveButton(R.string.protocol_mismatch_dialog_update_btn, (dialog, which) -> {
                Utilities.openAppInPlayStore(getContext(), getContext().getPackageName());
                dialog.dismiss();
            });
            dialogBuilder.setNeutralButton(R.string.protocol_mismatch_dialog_back_btn, (dialog, which) -> dialog.dismiss());
        }
        // Show alert dialog
        dialogBuilder.show();
    }
}
