package com.cod3rboy.apnashare.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;

import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.events.WiFiReceiverConnected;
import com.cod3rboy.apnashare.fragments.ProgressFilesFragment;
import com.cod3rboy.apnashare.fragments.ScanReceiverFragment;
import com.cod3rboy.apnashare.misc.FileSelectionQueue;
import com.cod3rboy.apnashare.models.BasicFile;
import com.cod3rboy.apnashare.misc.Utilities;
import com.cod3rboy.apnashare.models.TransmissionFile;
import com.cod3rboy.apnashare.transmission.TransmissionClient;
import com.cod3rboy.apnashare.transmission.TransmissionServer;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.net.InetAddress;
import java.util.ArrayList;


public class SendActivity extends BaseActivity {

    private ArrayList<TransmissionFile> mFilesToSend;
    private ProgressFilesFragment mProgressFragment;
    private ScanReceiverFragment mScanReceiverFragment;
    private View mScanFragmentView;
    private View mProgressFragmentView;
    private TransmissionClient mClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        mFilesToSend = new ArrayList<>();

        // Generate Progress files from selected files queue
        ArrayList<BasicFile> selectedFiles = FileSelectionQueue.getInstance().getAll();
        for (BasicFile file : selectedFiles)
            mFilesToSend.add(TransmissionFile.createFromSelectedFile(file));

        // Receive Fragment Views
        mScanFragmentView = findViewById(R.id.fragment_scan);
        mProgressFragmentView = findViewById(R.id.fragment_send);

        // Initially hide progress fragment and show scan fragment.
        mScanFragmentView.setVisibility(View.VISIBLE);
        mProgressFragmentView.setVisibility(View.GONE);

        mScanReceiverFragment = (ScanReceiverFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_scan);
        mProgressFragment = (ProgressFilesFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_send);

        // Start Permission Activity if any permission is missing
        if (!Utilities.isLocationEnabled() || !Utilities.isWiFiEnabled()) {
            int permissions = 0x00000000;
            permissions |= PermissionActivity.PERMISSION_LOCATION;
            permissions |= PermissionActivity.PERMISSION_WIFI;
            // Start PermissionActivity on when all permissions are not fulfilled
            Intent i = new Intent(this, PermissionActivity.class);
            i.putExtra(PermissionActivity.EXTRA_PERMISSIONS, permissions);
            startActivityForResult(i, PermissionActivity.PERMISSION_REQUEST_CODE);
        } else {
            mScanReceiverFragment.startWiFiScan();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PermissionActivity.PERMISSION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_CANCELED) {
                // User did not allow required permissions so finish this activity.
                finish();

            } else if (resultCode == Activity.RESULT_OK) {
                mScanReceiverFragment.startWiFiScan();
            }
        }
    }

    public ArrayList<TransmissionFile> getFilesToSend() {
        return mFilesToSend;
    }

    public void onReceiverConnected(WiFiReceiverConnected event) {
        // Initiate Transfer
        mProgressFragment.setReceiverSSID(event.getReceiverSSID());
        initiateTransfer(event.getIpAddress());
    }

    public void initiateTransfer(InetAddress receiverAddress) {
        // Start Files TransmissionClient here
        // Create a new Transmission Client to transmit all files
        mClient = new TransmissionClient(getApplicationContext(), mFilesToSend);
        mClient.beginTransmission(receiverAddress, TransmissionServer.SERVER_PORT);

        // Hide scan fragment view and show progress fragment view
        mScanFragmentView.setVisibility(View.GONE);
        mProgressFragmentView.setVisibility(View.VISIBLE);
    }

    public void exitSender() {
        if (mClient != null && mClient.isRunning()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_exit_confirm_title)
                    .setMessage(R.string.dialog_exit_confirm_description)
                    .setPositiveButton(R.string.dialog_exit_confirm_yes, (dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    })
                    .setNeutralButton(R.string.dialog_exit_confirm_no, (dialog, which) -> dialog.dismiss())
                    .show();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        exitSender();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mClient != null) mClient.setRunning(false);
    }
}
