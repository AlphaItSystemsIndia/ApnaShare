package com.cod3rboy.apnashare.managers;

import android.app.Application;
import android.net.wifi.WifiConfiguration;
import android.os.Build;

public abstract class HotspotManager {

    public enum HOTSPOT_STATE {
        HOTSPOT_STATE_TURNED_OFF,
        HOTSPOT_STATE_TURNED_ON,
    }

    public interface HotspotListener {
        void onStarted(WifiConfiguration configuration);

        void onFailed();

        void onStateChanged(HOTSPOT_STATE hotspotState);
    }

    public static HotspotManager getInstance(Application appContext) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            return new LocalHotspotManager(appContext);
        else
            return new GlobalHotspotManager(appContext);
    }

    protected HotspotListener mHotspotListener;


    public abstract void startHotspot();

    public abstract void stopHotspot();

    public void registerListener(HotspotListener listener) {
        mHotspotListener = listener;
    }

    public void unregisterListener() {
        mHotspotListener = null;
    }
}
