package com.cod3rboy.apnashare.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import com.cod3rboy.apnashare.misc.Utilities;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class GlobalHotspotManager extends HotspotManager {
    private static final String LOG_TAG = GlobalHotspotManager.class.getSimpleName();

    private WifiManager mWifiMgr;
    private WifiConfiguration mOldHotspotConfig;
    private WifiConfiguration mHotspotConfig;
    private String mRandPreSharedKey;
    private boolean mHotspotFirstStart;
    private HotspotStateReciever mHotspotStateReceiver;

    GlobalHotspotManager(Context context) {
        mWifiMgr = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mOldHotspotConfig = null;
        mHotspotConfig = null;
        mHotspotFirstStart = false;
        mHotspotStateReceiver = new HotspotStateReciever(context);
    }

    @Override
    public void startHotspot() {
        mHotspotFirstStart = false;
        // Turn off wifi station mode first
        if (isStationModeActive()) turnOffStationMode();
        configureHotspot();
        // Start Hotspot with new configuration
        try {
            Method startWifiApMethod = mWifiMgr.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            boolean hotspotEnabled = (boolean) startWifiApMethod.invoke(mWifiMgr, mHotspotConfig, Boolean.TRUE);
            if (!hotspotEnabled) {
                if (mHotspotListener != null) mHotspotListener.onFailed();
            } else {
                mHotspotStateReceiver.registerHotspotReceiver();
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
            ignore.printStackTrace();
            if (mHotspotListener != null) mHotspotListener.onFailed();
        }
    }

    @Override
    public void stopHotspot() {
        if (mOldHotspotConfig != null) restoreOldHotspotConfig();
        if (mHotspotStateReceiver.isReceiverRegistered())
            mHotspotStateReceiver.unregisterHotspotReceiver();
    }

    private boolean isHotspotOn() {
        try {
            Method method = mWifiMgr.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(mWifiMgr);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private void configureHotspot() {
        try {
            // Save old hotspot configuration
            Method wifiApConfigMethod = mWifiMgr.getClass().getMethod("getWifiApConfiguration");
            wifiApConfigMethod.setAccessible(true);
            mOldHotspotConfig = (WifiConfiguration) wifiApConfigMethod.invoke(mWifiMgr);
            // Create new hotspot configuration
            mHotspotConfig = new WifiConfiguration();

            mHotspotConfig.SSID = Utilities.randomSSID();
            mHotspotConfig.status = WifiConfiguration.Status.ENABLED;
            mHotspotConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            mHotspotConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            mRandPreSharedKey = Utilities.randomPassword();
            mHotspotConfig.preSharedKey = mRandPreSharedKey;
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    private void restoreOldHotspotConfig() {
        try {
            if (isHotspotOn()) {
                Method stopWifiApMethod = mWifiMgr.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                stopWifiApMethod.invoke(mWifiMgr, mHotspotConfig, Boolean.FALSE);
            }
            Method setWifiApConfigMethod = mWifiMgr.getClass().getMethod("setWifiApConfiguration", WifiConfiguration.class);
            setWifiApConfigMethod.invoke(mWifiMgr, mOldHotspotConfig);
        } catch (Exception ignore) {
            ignore.printStackTrace();
        }
    }

    private boolean isStationModeActive() {
        int wifiState = mWifiMgr.getWifiState();
        return wifiState == WifiManager.WIFI_STATE_ENABLED
                || wifiState == WifiManager.WIFI_STATE_ENABLING;
    }

    private boolean turnOffStationMode() {
        return mWifiMgr.setWifiEnabled(false);
    }


    class HotspotStateReciever extends BroadcastReceiver {
        public static final String ACTION_HOTSPOT_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED";

        private Context mContext;
        private IntentFilter mIntentFilter;
        private boolean mReceiverRegistered;

        public HotspotStateReciever(Context context) {
            mContext = context.getApplicationContext();
            mIntentFilter = new IntentFilter();
            mIntentFilter.addAction(ACTION_HOTSPOT_STATE_CHANGED);
            mReceiverRegistered = false;
        }

        public boolean isReceiverRegistered() {
            return mReceiverRegistered;
        }

        public void registerHotspotReceiver() {
            mContext.registerReceiver(this, mIntentFilter);
            mReceiverRegistered = true;
        }

        public void unregisterHotspotReceiver() {
            mContext.unregisterReceiver(this);
            mReceiverRegistered = false;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(ACTION_HOTSPOT_STATE_CHANGED)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                if (!mHotspotFirstStart && WifiManager.WIFI_STATE_ENABLED == state % 10) {
                    // Hotspot successfully started for first time
                    mHotspotFirstStart = true;
                    if (mHotspotListener != null) mHotspotListener.onStarted(mHotspotConfig);
                } else if (mHotspotFirstStart) {
                    if (WifiManager.WIFI_STATE_ENABLED == state % 10) {
                        if (mHotspotListener != null)
                            mHotspotListener.onStateChanged(HOTSPOT_STATE.HOTSPOT_STATE_TURNED_ON);
                    } else if (WifiManager.WIFI_STATE_DISABLED == state % 10) {
                        if (mHotspotListener != null)
                            mHotspotListener.onStateChanged(HOTSPOT_STATE.HOTSPOT_STATE_TURNED_OFF);
                    }
                }
            }
        }
    }
}
