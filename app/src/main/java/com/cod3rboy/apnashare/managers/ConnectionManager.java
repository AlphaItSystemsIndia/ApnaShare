package com.cod3rboy.apnashare.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;

import com.cod3rboy.apnashare.App;
import com.cod3rboy.apnashare.events.WiFiReceiverConnected;
import com.cod3rboy.apnashare.events.WiFiReceiverDisconnected;
import com.cod3rboy.apnashare.events.WiFiScanComplete;
import com.cod3rboy.apnashare.misc.Utilities;

import org.greenrobot.eventbus.EventBus;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public abstract class ConnectionManager {
    private static final String LOG_TAG = ConnectionManager.class.getSimpleName();
    protected static final String RECEIVER_PREFIX = "AndroidShare";
    protected Context mAppContext;
    private WiFiScanReceiver mScanReceiver;
    protected WifiManager mWiFiMgr;
    protected WifiManager.WifiLock mWifiLock;

    public static ConnectionManager getInstance() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            return new NewConnectionManager();
        else
            return new LegacyConnectionManager();
    }

    protected ConnectionManager() {
        mAppContext = App.getInstance().getApplicationContext();
        mScanReceiver = new WiFiScanReceiver();
        mWiFiMgr = (WifiManager) mAppContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiLock = mWiFiMgr.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, ConnectionManager.class.getSimpleName());
        mWifiLock.setReferenceCounted(false);
    }

    public WifiManager.WifiLock getWifiLock() {
        return mWifiLock;
    }

    private void registerScanReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mAppContext.registerReceiver(mScanReceiver, filter);
    }

    private void unregisterScanReceiver() {
        mAppContext.unregisterReceiver(mScanReceiver);
    }

    public void scanReceivers() {
        if (!Utilities.isWiFiEnabled()) return;
        registerScanReceiver();
        boolean success = mWiFiMgr.startScan();
        if (!success) scanComplete();
    }

    private void scanComplete() {
        unregisterScanReceiver();
        List<ScanResult> results = mWiFiMgr.getScanResults();
        ArrayList<String> receiverSSIDs = new ArrayList<>();
        for (ScanResult result : results) {
            if (result.SSID.toLowerCase().contains(RECEIVER_PREFIX.toLowerCase())) {
                String receiverSSID = Utilities.stripQuotes(result.SSID);
                receiverSSIDs.add(receiverSSID);
            }
        }
        notifyScanComplete(receiverSSIDs);
    }

    private void notifyScanComplete(ArrayList<String> receiverSSIDs) {
        EventBus bus = EventBus.getDefault();
        if (bus.hasSubscriberForEvent(WiFiScanComplete.class))
            bus.post(new WiFiScanComplete(receiverSSIDs));
    }

    protected void notifyReceiverConnected(InetAddress receiverIPAddress, String preSharedKey) {
        EventBus bus = EventBus.getDefault();
        if (bus.hasSubscriberForEvent(WiFiReceiverConnected.class))
            bus.post(new WiFiReceiverConnected(receiverIPAddress, preSharedKey));
    }

    protected void notifyReceiverDisconnected() {
        EventBus bus = EventBus.getDefault();
        if (bus.hasSubscriberForEvent(WiFiReceiverDisconnected.class))
            bus.post(new WiFiReceiverDisconnected());
    }

    public abstract void connectToReceiver(String receiverSSID, String preSharedKey);

    public void release() {
        if (mWifiLock != null && mWifiLock.isHeld())
            mWifiLock.release();
    }

    class WiFiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null
                    && intent.getAction().matches(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                scanComplete();
            }
        }
    }
}
