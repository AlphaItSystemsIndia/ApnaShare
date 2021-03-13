package com.cod3rboy.apnashare.fragments;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.activities.SendActivity;
import com.cod3rboy.apnashare.adapters.ReceiversAdapter;
import com.cod3rboy.apnashare.events.WiFiReceiverConnected;
import com.cod3rboy.apnashare.events.WiFiReceiverDisconnected;
import com.cod3rboy.apnashare.events.WiFiScanComplete;
import com.cod3rboy.apnashare.managers.ConnectionManager;
import com.cod3rboy.apnashare.misc.Utilities;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

public class ScanReceiverFragment extends Fragment {
    private static final int MAX_RETRIES = 3;
    private RecyclerView mReceiversView;
    private ReceiversAdapter mReceiversAdapter;
    private ConnectionManager mWiFiConMgr;
    private LottieAnimationView mQrScanView;
    private LottieAnimationView mScanAnimation;
    private View mRescanView;
    private TextView mStatusView;
    private LottieAnimationView mRescanLottieView;
    private int mRetries;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mWiFiConMgr = ConnectionManager.getInstance();
        View v = inflater.inflate(R.layout.fragment_scan_receiver, container, false);
        mStatusView = v.findViewById(R.id.status);
        mScanAnimation = v.findViewById(R.id.scan_view);
        mReceiversView = v.findViewById(R.id.rv_receivers);
        mRescanView = v.findViewById(R.id.view_scan_again);
        mRescanLottieView = mRescanView.findViewById(R.id.rescan_view);
        mQrScanView = v.findViewById(R.id.qr_code);
        mReceiversView.setLayoutManager(new LinearLayoutManager(getContext()));
        mReceiversView.setHasFixedSize(true);
        mReceiversAdapter = new ReceiversAdapter(getContext());
        mReceiversAdapter.registerItemClickListener(position -> showReceiverConnectDialog(mReceiversAdapter.getReceiverAtPosition(position)));
        mReceiversView.setAdapter(mReceiversAdapter);

        // Set up qr code scan click listener
        mQrScanView.setOnClickListener(view -> {
            // Start QR Code scan to get password and connect to Receiver
            IntentIntegrator integrator = IntentIntegrator.forSupportFragment(ScanReceiverFragment.this);
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
            integrator.setPrompt(getString(R.string.qr_scan_prompt));
            integrator.setCameraId(0);  // Use a specific camera of the device
            integrator.setBeepEnabled(false);
            integrator.setOrientationLocked(true);
            integrator.setBarcodeImageEnabled(true);
            integrator.initiateScan();
        });

        mRetries = 0;
        // Setup click listener on lottie rescan view to scan receivers again
        mRescanLottieView.setOnClickListener((view) -> {
            if (mRetries < MAX_RETRIES) {
                mRetries++;
                mRescanLottieView.playAnimation();
                startWiFiScan();
            } else {
                // Retries limit reached
                new MaterialAlertDialogBuilder(getContext())
                        .setTitle(R.string.scan_error_title)
                        .setMessage(R.string.scan_error_msg)
                        .setPositiveButton(R.string.scan_error_dismiss_btn, (dialog, which) -> {
                            dialog.dismiss();
                        }).show();
            }
        });
        return v;
    }

    public void startWiFiScan() {
        mStatusView.setText(R.string.status_scan);
        mScanAnimation.playAnimation();
        mWiFiConMgr.scanReceivers();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String body = result.getContents();
                if (Utilities.isValidQrBody(body)) {
                    String[] receiverData = Utilities.splitQrBody(body);
                    mWiFiConMgr.connectToReceiver(receiverData[0], receiverData[1]);
                } else {
                    // Invalid QR Code scanned
                    new MaterialAlertDialogBuilder(getContext())
                            .setTitle(R.string.invalid_qr_title)
                            .setMessage(R.string.invalid_qr_msg)
                            .setPositiveButton(R.string.invalid_qr_close_btn, (dialog, which) -> dialog.dismiss())
                            .show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void receiversNotFound() {
        mStatusView.setText(R.string.status_scan_receiver_not_found);
        mScanAnimation.cancelAnimation();
        mReceiversView.setVisibility(View.GONE);
        mRescanView.setVisibility(View.VISIBLE);
    }

    private void receiversFound() {
        mStatusView.setText(R.string.status_scan_receiver_found);
        mReceiversView.setVisibility(View.VISIBLE);
        mRescanView.setVisibility(View.GONE);
    }

    private void showReceiverConnectDialog(String receiver) {
        View dialogLayout = LayoutInflater.from(getContext()).inflate(R.layout.dialog_prompt_password, null);
        EditText passwordEditText = dialogLayout.findViewById(R.id.dialog_input_pass);
        TextView receiverTextView = dialogLayout.findViewById(R.id.receiver_name);
        receiverTextView.setText(receiver);
        new MaterialAlertDialogBuilder(getContext())
                .setView(dialogLayout)
                .setPositiveButton(R.string.pwd_dialog_connect_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String preSharedKey = passwordEditText.getText().toString();
                        if (!preSharedKey.trim().isEmpty()) {
                            mWiFiConMgr.connectToReceiver(receiver, preSharedKey);
                        }
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.pwd_dialog_cancel_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    public String getConnectedReceiver() {
        return mReceiversAdapter.getConnectedReceiver();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Register Event Bus
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mWiFiConMgr.release();
        // Unregister Event Bus
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnWiFiScanComplete(WiFiScanComplete event) {
        mReceiversAdapter.clearAll();
        ArrayList<String> receiverSSIDs = event.getReceiverSSIDs();
        if (receiverSSIDs.size() <= 0) {
            receiversNotFound();
            return;
        }
        receiversFound();
        for (String ssid : receiverSSIDs) mReceiversAdapter.addReceiver(ssid);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWiFiReceiverConnected(WiFiReceiverConnected event) {
        mReceiversAdapter.setConnectedReceiver(event.getReceiverSSID());
        ((SendActivity) getActivity()).onReceiverConnected(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWiFiReceiverDisconnected(WiFiReceiverDisconnected event) {
        mReceiversAdapter.setConnectedReceiver(null);
    }
}
