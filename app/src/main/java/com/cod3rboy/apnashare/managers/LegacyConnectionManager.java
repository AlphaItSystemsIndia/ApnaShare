package com.cod3rboy.apnashare.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.cod3rboy.apnashare.misc.Utilities;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class LegacyConnectionManager extends ConnectionManager {
    private static final String LOG_TAG = LegacyConnectionManager.class.getSimpleName();

    private String mReceiverSSID;
    private WiFiNetStateReceiver mStateReceiver;
    private int mReceiverNetworkId = -1;

    @Override
    public void connectToReceiver(String receiverSSID, String preSharedKey) {
        if (!Utilities.isWiFiEnabled()) return;
        // Acquire Wifi Lock
        if (getWifiLock() != null) getWifiLock().acquire();
        mWiFiMgr.disconnect();
        Log.d(LOG_TAG, "Connecting to receiver : " + receiverSSID);
        WifiConfiguration receiverConfig = new WifiConfiguration();
        receiverConfig.SSID = "\"" + receiverSSID + "\"";
        receiverConfig.preSharedKey = "\"" + preSharedKey + "\"";
        receiverConfig.priority = Integer.MAX_VALUE;
        mWiFiMgr.addNetwork(receiverConfig);
        List<WifiConfiguration> list = mWiFiMgr.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && Utilities.stripQuotes(i.SSID).equals(receiverSSID)) {
                registerWiFiNetStateReceiver();
                mReceiverSSID = receiverSSID;
                receiverConfig.networkId = i.networkId;
                mReceiverNetworkId = i.networkId;
                mWiFiMgr.enableNetwork(i.networkId, true);
                break;
            }
        }
    }

    private void registerWiFiNetStateReceiver() {
        Log.d(LOG_TAG, "Registering WiFi Network State Receiver");
        mStateReceiver = new WiFiNetStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mAppContext.registerReceiver(mStateReceiver, filter);
    }

    private void unregisterWiFiNetStateReceiver() {
        if (mStateReceiver != null) {
            Log.d(LOG_TAG, "Unregistering WiFi Network State Receiver");
            mAppContext.unregisterReceiver(mStateReceiver);
            mStateReceiver = null;
        }
    }

    private void removeReceiverFromSavedNetworks() {
        if (mReceiverNetworkId == -1) return;
        mWiFiMgr.removeNetwork(mReceiverNetworkId);
        mReceiverNetworkId = -1;
    }

    @Override
    public void release() {
        super.release();
        unregisterWiFiNetStateReceiver();
        removeReceiverFromSavedNetworks();
    }

    private class WiFiNetStateReceiver extends BroadcastReceiver {
        private boolean receiverConnected = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().matches(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                Log.d(LOG_TAG, "WiFi Network State Changed");
                NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (info != null && info.getType() == ConnectivityManager.TYPE_WIFI) {
                    boolean wifiConnected = info.getState() == NetworkInfo.State.CONNECTED;
                    boolean wifiDisconnecting = info.getState() == NetworkInfo.State.DISCONNECTING;
                    // invoke network state change callback
                    if (!receiverConnected && wifiConnected) {
                        Log.d(LOG_TAG, "WiFi Network Receiver Connected");
                        WifiInfo wifiInfo = mWiFiMgr.getConnectionInfo();
                        String ssid = Utilities.stripQuotes(wifiInfo.getSSID());
                        if (ssid.equals(Utilities.stripQuotes(mReceiverSSID))) {
                            DhcpInfo dhcpInfo = mWiFiMgr.getDhcpInfo();
                            int rawIp = dhcpInfo.serverAddress;
                            String ipAddress = "";
                            ipAddress += String.valueOf(rawIp & 0x000000FF) + ".";
                            rawIp = rawIp >> 8;
                            ipAddress += String.valueOf(rawIp & 0x000000FF) + ".";
                            rawIp = rawIp >> 8;
                            ipAddress += String.valueOf(rawIp & 0x000000FF) + ".";
                            rawIp = rawIp >> 8;
                            ipAddress += String.valueOf(rawIp & 0x000000FF);
                            try {
                                Log.d(LOG_TAG, "Connected to hotspot with ip address : " + ipAddress);
                                receiverConnected = true;
                                notifyReceiverConnected(InetAddress.getByName(ipAddress), ssid);
                            } catch (UnknownHostException ex) {
                                ex.printStackTrace();
                            }
                        }
                    } else if (wifiDisconnecting && receiverConnected) {
                        Log.d(LOG_TAG, "WiFi Network Receiver Disconnected");
                        receiverConnected = false;
                        unregisterWiFiNetStateReceiver();
                        notifyReceiverDisconnected();
                    }
                }
            }
        }
    }
}
