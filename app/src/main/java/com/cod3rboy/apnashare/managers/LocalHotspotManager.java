package com.cod3rboy.apnashare.managers;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.O)
public class LocalHotspotManager extends HotspotManager {
    private static final String LOG_TAG = LocalHotspotManager.class.getSimpleName();
    private static final int FIRST_START_DELAY = 500; // 500ms delay before starting hotspot for first time
    private WifiManager mWifiMgr;
    private WifiManager.LocalOnlyHotspotReservation mReservation;
    private boolean mStarting;
    private boolean mFirstStart;
    private Handler mMainHandler;

    public LocalHotspotManager(Context context) {
        mWifiMgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mReservation = null;
        mStarting = false;
        mFirstStart = true;
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void startHotspot() {
        // Return if hotspot is already started or is being started
        if (mReservation != null || mStarting) return;
        int startDelay = mFirstStart ? FIRST_START_DELAY : 0;
        if (mFirstStart) mFirstStart = false;
        // Start Local only hotspot
        mStarting = true;
        mMainHandler.postDelayed(() -> {
            try {
                mWifiMgr.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
                    @Override
                    public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                        mReservation = reservation;
                        // @todo wifi configuration is deprecated in API Level 30 (R)
                        WifiConfiguration wifiConfig = mReservation.getWifiConfiguration();
                        if (mHotspotListener != null) mHotspotListener.onStarted(wifiConfig);
                        mStarting = false;
                    }

                    @Override
                    public void onStopped() {
                        clearHotspotReservation();
                        if (mHotspotListener != null)
                            mHotspotListener.onStateChanged(HOTSPOT_STATE.HOTSPOT_STATE_TURNED_OFF);
                    }

                    @Override
                    public void onFailed(int reason) {
                        if (mHotspotListener != null) mHotspotListener.onFailed();
                        mStarting = false;
                        Log.e(LOG_TAG, "Failed to start LocalOnlyHotspot. Reason Code = " + reason);
                    }
                }, null);
            } catch (IllegalStateException ex) {
                // This exception is thrown if there is already an active LocalOnlyHotspot request.
                ex.printStackTrace();
                if (mHotspotListener != null) mHotspotListener.onFailed();
                mStarting = false;
            }
        }, startDelay);
    }

    @Override
    public void stopHotspot() {
        clearHotspotReservation();
    }

    private void clearHotspotReservation() {
        if (mReservation != null) {
            mReservation.close();
            mReservation = null;
        }
    }
}
