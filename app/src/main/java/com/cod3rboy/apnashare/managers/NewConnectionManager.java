package com.cod3rboy.apnashare.managers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.cod3rboy.apnashare.misc.Utilities;

import java.net.InetAddress;
import java.net.UnknownHostException;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class NewConnectionManager extends ConnectionManager {
    private WiFiNetworkCallback mNetworkCallback;


    @Override
    public void connectToReceiver(String receiverSSID, String preSharedKey) {
        if (!Utilities.isWiFiEnabled()) return;
        // Acquire Wifi Lock
        if (getWifiLock() != null) getWifiLock().acquire();

        WifiNetworkSpecifier receiverSpecifier = new WifiNetworkSpecifier.Builder()
                .setSsid(receiverSSID)
                .setWpa2Passphrase(preSharedKey)
                .build();

        NetworkRequest.Builder wifiNetworkRequestBuilder = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                .setNetworkSpecifier(receiverSpecifier);

        if (receiverSSID.contains("_")) { // @todo use regex to match local only hotspot
            // Remove Internet Capability for local only hotspot
            wifiNetworkRequestBuilder.removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }

        ConnectivityManager cm = (ConnectivityManager) mAppContext.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            mNetworkCallback = new WiFiNetworkCallback();
            cm.requestNetwork(wifiNetworkRequestBuilder.build(), mNetworkCallback, new Handler(Looper.getMainLooper()));
        }
    }

    private void unregisterNetworkCallback() {
        if (mNetworkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) mAppContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                cm.unregisterNetworkCallback(mNetworkCallback);
                mNetworkCallback = null;
            }
        }
    }

    @Override
    public void release() {
        super.release();
        unregisterNetworkCallback();
    }


    private class WiFiNetworkCallback extends ConnectivityManager.NetworkCallback {
        @Override
        public void onAvailable(@NonNull Network network) {
            ConnectivityManager cm = (ConnectivityManager) mAppContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm != null) cm.bindProcessToNetwork(network);
            // invoke network state change callback
            WifiInfo wifiInfo = mWiFiMgr.getConnectionInfo();
            DhcpInfo dhcpInfo = mWiFiMgr.getDhcpInfo();
            String ssid = Utilities.stripQuotes(wifiInfo.getSSID());
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
                InetAddress receiverIPAddress = network.getByName(ipAddress);
                notifyReceiverConnected(receiverIPAddress, ssid);
            } catch (UnknownHostException ex) {
                ex.printStackTrace();
            }
        }

        @Override
        public void onLost(@NonNull Network network) {
            unregisterNetworkCallback();
            notifyReceiverDisconnected();
        }
    }
}
