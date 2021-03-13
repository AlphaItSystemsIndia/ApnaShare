package com.cod3rboy.apnashare.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.airbnb.lottie.LottieAnimationView;
import com.cod3rboy.apnashare.R;
import com.cod3rboy.apnashare.adapters.TransmissionFilesAdapter;
import com.cod3rboy.apnashare.managers.HotspotManager;
import com.cod3rboy.apnashare.misc.Utilities;
import com.cod3rboy.apnashare.models.TransmissionFile;
import com.cod3rboy.apnashare.transmission.TransmissionServer;
import com.cod3rboy.apnashare.transmission.events.ProtocolMismatch;
import com.cod3rboy.apnashare.transmission.events.server.ClientConnected;
import com.cod3rboy.apnashare.transmission.events.server.MetaDataReceived;
import com.cod3rboy.apnashare.transmission.events.ProgressUpdate;
import com.cod3rboy.apnashare.transmission.events.server.ServerFinished;
import com.cod3rboy.apnashare.transmission.events.server.TUnitReceptionCompleted;
import com.cod3rboy.apnashare.transmission.events.server.TUnitReceptionStarted;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;


public class ReceiveActivity extends BaseActivity implements HotspotManager.HotspotListener {
    public static final String LOG_TAG = ReceiveActivity.class.getSimpleName();
    private static final int MAX_RETRIES = 2;
    private TextView mHotspotNameTextView;
    private TextView mHotspotKeyTextView;
    private ImageView mQrCodeImageView;
    private HotspotManager mHsMgr;
    private RecyclerView mMetaFilesRecyclerView;
    private TransmissionFilesAdapter mTransmissionFilesAdapter;
    private TransmissionServer mServer;
    private TextView mStatusTextView;
    private View mHotspotDetailsLayout;
    private View mReloadLayout;
    private TextView mBytesReceived;
    private TextView mBytesReceivedUnit;
    private View mProgressFilesView;
    private int mRetries = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Override default status bar color here
        getWindow().setStatusBarColor(getResources().getColor(R.color.colorAccent));
        // Override default navigation bar color here
        setNavigationColorAccent();
        // Register for subscribed events
        EventBus.getDefault().register(this);
        setContentView(R.layout.activity_receive);
        mStatusTextView = findViewById(R.id.tv_status);
        mHotspotDetailsLayout = findViewById(R.id.layout_hotspot_details);
        mReloadLayout = findViewById(R.id.layout_reload);
        mProgressFilesView = findViewById(R.id.layout_receive_items);
        mHotspotNameTextView = findViewById(R.id.tv_hotspot_name);
        mHotspotKeyTextView = findViewById(R.id.tv_hotspot_key);
        mQrCodeImageView = findViewById(R.id.iv_qr_code);
        mBytesReceived = mProgressFilesView.findViewById(R.id.tv_size_total);
        mBytesReceivedUnit = mProgressFilesView.findViewById(R.id.tv_size_unit);
        mMetaFilesRecyclerView = findViewById(R.id.rv_meta_files);
        mMetaFilesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mMetaFilesRecyclerView.setHasFixedSize(true);
        ((SimpleItemAnimator) mMetaFilesRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
        mTransmissionFilesAdapter = new TransmissionFilesAdapter(this);
        mMetaFilesRecyclerView.setAdapter(mTransmissionFilesAdapter);

        // Set close button click handler
        ((ImageButton) findViewById(R.id.btn_close)).setOnClickListener(v -> exitReceiver());

        // Set Reload button click listener
        LottieAnimationView reloadAnimView = mReloadLayout.findViewById(R.id.reload_view);
        mReloadLayout.setOnClickListener(v -> {
            reloadAnimView.playAnimation();
            startReceiver();
            if (mRetries < MAX_RETRIES) mRetries++;
        });

        // Start Permission Activity if any permission is missing
        if (!Utilities.isLocationEnabled()
                || !Utilities.canWriteSystemSettings()
                || !Utilities.isStoragePermissionGranted()) {
            int permissions = 0x00000000;
            permissions |= PermissionActivity.PERMISSION_LOCATION;
            if (!Utilities.canWriteSystemSettings())
                permissions |= PermissionActivity.PERMISSION_SYSTEM_SETTINGS;
            if (!Utilities.isStoragePermissionGranted())
                permissions |= PermissionActivity.PERMISSION_STORAGE;
            // Start PermissionActivity on when all permissions are not fulfilled
            Intent i = new Intent(this, PermissionActivity.class);
            i.putExtra(PermissionActivity.EXTRA_PERMISSIONS, permissions);
            startActivityForResult(i, PermissionActivity.PERMISSION_REQUEST_CODE);
        } else {
            startReceiver();
        }
    }

    private void setNavigationColorAccent() {
        getWindow().setNavigationBarColor(getResources().getColor(R.color.colorAccent));
    }

    private void setNavigationColorPrimarySurface() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
            getWindow().setNavigationBarColor(getColor(R.color.primarySurfaceColor));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PermissionActivity.PERMISSION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                startReceiver();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        exitReceiver();
    }

    private void setHotspotDetailsVisible(boolean visible) {
        if (visible) {
            mHotspotDetailsLayout.animate()
                    .alpha(1)
                    .setDuration(500)
                    .start();
            mHotspotDetailsLayout.setAlpha(1);
            mHotspotDetailsLayout.setVisibility(View.VISIBLE);
        } else {
            mHotspotDetailsLayout.animate()
                    .alpha(0)
                    .setDuration(300)
                    .start();
            mHotspotDetailsLayout.setAlpha(0);
            mHotspotDetailsLayout.setVisibility(View.GONE);
        }
    }

    private void setReloadVisible(boolean visible) {
        if (visible) {
            mReloadLayout.animate()
                    .alpha(1)
                    .setDuration(500)
                    .start();
            mReloadLayout.setAlpha(1);
            mReloadLayout.setVisibility(View.VISIBLE);
        } else {
            mReloadLayout.animate()
                    .alpha(0)
                    .setDuration(300)
                    .start();
            mReloadLayout.setAlpha(0);
            mReloadLayout.setVisibility(View.GONE);
        }
    }

    private void setProgressFilesVisible(boolean visible) {
        if (visible) {
            mProgressFilesView.animate()
                    .alpha(1)
                    .setDuration(500)
                    .start();
            mProgressFilesView.setAlpha(1);
            mProgressFilesView.setVisibility(View.VISIBLE);
            setNavigationColorPrimarySurface();
        } else {
            mProgressFilesView.animate()
                    .alpha(0)
                    .setDuration(300)
                    .start();
            mProgressFilesView.setAlpha(0);
            mProgressFilesView.setVisibility(View.GONE);
            setNavigationColorAccent();
        }
    }

    public void startReceiver() {
        if (mRetries >= MAX_RETRIES) {
            // When retries limit reached. Show hardware failure dialog.
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.hotspot_error_title)
                    .setMessage(R.string.hotspot_error_message)
                    .setPositiveButton(R.string.hotspot_error_dismiss_btn, (dialog, which) -> {
                        dialog.dismiss();
                    }).show();
            return;
        }
        if (mHsMgr == null) {
            mHsMgr = HotspotManager.getInstance(getApplication());
            mHsMgr.registerListener(this);
        }
        mHsMgr.startHotspot();
        mStatusTextView.setText(R.string.hotspot_status_starting);
        if (mServer == null) {
            mServer = new TransmissionServer(getApplicationContext());
            mServer.listen();
        }
    }

    private void exitReceiver() {
        if (mServer != null && mServer.isRunning()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_exit_confirm_title)
                    .setMessage(R.string.dialog_exit_confirm_description)
                    .setPositiveButton(R.string.dialog_exit_confirm_yes, (dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    })
                    .setNeutralButton(R.string.dialog_exit_confirm_no, (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        } else {
            finish();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister for events
        EventBus.getDefault().unregister(this);

        if (mServer != null) {
            mServer.shutdown();
            mServer = null;
        }
        if (mHsMgr != null) {
            mHsMgr.stopHotspot();
            mHsMgr = null;
        }
    }

    @Override
    public void onStarted(WifiConfiguration configuration) {
        String ssid = configuration.SSID;
        if (ssid.startsWith("\""))
            ssid = Utilities.stripQuotes(ssid);
        String password = configuration.preSharedKey;
        if (password.startsWith("\""))
            password = Utilities.stripQuotes(password);
        mHotspotNameTextView.setText(ssid);
        mHotspotKeyTextView.setText(password);
        try {
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.encodeBitmap(Utilities.generateQrBody(ssid, password), BarcodeFormat.QR_CODE, 200, 200);
            // change bitmap background to transparent and qr code color to white
            for (int i = 0; i < bitmap.getHeight(); i++) {
                for (int j = 0; j < bitmap.getWidth(); j++) {
                    int pixelColor = bitmap.getPixel(i, j);
                    if (pixelColor == 0xFFFFFFFF) {
                        bitmap.setPixel(i, j, 0x00000000);
                    }
                }
            }
            mQrCodeImageView.setImageBitmap(bitmap);
            mQrCodeImageView.setVisibility(View.VISIBLE);
            setHotspotDetailsVisible(true);
            setReloadVisible(false);
            mStatusTextView.setText(R.string.hotspot_status_waiting);
        } catch (Exception e) {
            e.printStackTrace();
            setHotspotDetailsVisible(false);
            setReloadVisible(true);
            mStatusTextView.setText(R.string.hotspot_status_error);
        }
    }

    @Override
    public void onFailed() {
        setHotspotDetailsVisible(false);
        setReloadVisible(true);
        mStatusTextView.setText(R.string.hotspot_status_error);
    }

    @Override
    public void onStateChanged(HotspotManager.HOTSPOT_STATE hotspotState) {
       /* if (hotspotState == HotspotManager.HOTSPOT_STATE.HOTSPOT_STATE_TURNED_ON) {
            Toast.makeText(this, "Hotspot turned on!", Toast.LENGTH_SHORT).show();
        } else if (hotspotState == HotspotManager.HOTSPOT_STATE.HOTSPOT_STATE_TURNED_OFF) {
            Toast.makeText(this, "Hotspot turned off!", Toast.LENGTH_SHORT).show();
        }*/
        if (hotspotState == HotspotManager.HOTSPOT_STATE.HOTSPOT_STATE_TURNED_OFF) {
            setHotspotDetailsVisible(false);
            setReloadVisible(true);
            mStatusTextView.setText(R.string.hotspot_status_failure);
        }
    }

    private void globalProgressUpdate(long totalBytesReceived) {
        // Update Bytes Received View
        String[] dataReceived = Utilities.convertBytesToString(totalBytesReceived).split("\\s+");
        mBytesReceived.setText(dataReceived[0]);
        mBytesReceivedUnit.setText(dataReceived[1]);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onClientConnected(ClientConnected event) {
        mStatusTextView.setText(R.string.hotspot_status_receiving);
        setProgressFilesVisible(true);
        setHotspotDetailsVisible(false);
        globalProgressUpdate(0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMetaDataReceived(MetaDataReceived event) {
        mTransmissionFilesAdapter.setFilesToReceive(event.getFilesToReceive());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerFinished(ServerFinished event) {
        if (!event.wasSuccessful()) {
            mTransmissionFilesAdapter.markAllPendingMetaFilesAsFailed();
            Log.e(LOG_TAG, "Server shutdown with failure");
            mStatusTextView.setText(R.string.hotspot_status_failed);
        } else {
            Log.e(LOG_TAG, "Server shutdown successfully");
            mStatusTextView.setText(R.string.hotspot_status_received);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTUnitReceptionStarted(TUnitReceptionStarted event) {
        mTransmissionFilesAdapter.setFileState(event.getFile(), TransmissionFile.State.PROGRESSING);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onTUnitReceptionCompleted(TUnitReceptionCompleted event) {
        mTransmissionFilesAdapter.setFileState(event.getFile(), TransmissionFile.State.SUCCESS);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onProgressUpdate(ProgressUpdate event) {
        globalProgressUpdate(event.getTotalBytesProcessed());
        mTransmissionFilesAdapter.setProgressUpdate(event);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onProtocolMismatch(ProtocolMismatch event) {
        // Show Protocol mismatched alert dialog
        int lv = event.getLocalProtocolVersion(); // Local Version
        int rv = event.getRemoteProtocolVersion(); // Remote Version
        String msgForOldVersion = getString(R.string.protocol_mismatch_dialog_msg_for_old);
        String msgForNewVersion = getString(R.string.protocol_mismatch_dialog_msg_for_new);
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this);
        dialogBuilder.setTitle(R.string.protocol_mismatch_dialog_title);
        dialogBuilder.setOnDismissListener(dialog -> exitReceiver());
        if (lv > rv) {
            dialogBuilder.setMessage(msgForNewVersion);
            dialogBuilder.setPositiveButton(R.string.protocol_mismatch_dialog_ok_btn, (dialog, which) -> dialog.dismiss());
        } else if (rv > lv) {
            dialogBuilder.setMessage(msgForOldVersion);
            dialogBuilder.setPositiveButton(R.string.protocol_mismatch_dialog_update_btn, (dialog, which) -> {
                Utilities.openAppInPlayStore(this, getPackageName());
                dialog.dismiss();
            });
            dialogBuilder.setNeutralButton(R.string.protocol_mismatch_dialog_back_btn, (dialog, which) -> dialog.dismiss());
        }
        // Show alert dialog
        dialogBuilder.show();
    }
}
