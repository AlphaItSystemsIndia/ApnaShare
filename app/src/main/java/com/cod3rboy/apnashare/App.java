package com.cod3rboy.apnashare;

import android.app.Application;

import com.cod3rboy.crashbottomsheet.CrashBottomSheet;

import org.greenrobot.eventbus.EventBus;

public class App extends Application {
    private static App singleton = null;

    public static App getInstance() {
        return singleton;
    }

    public App() {
        singleton = this;

        if (!BuildConfig.DEBUG)
            // Register crash bottom sheet
            CrashBottomSheet.register(this);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        // Install subscribers index in default Event Bus.
        EventBus.builder().addIndex(new EventBusIndex()).installDefaultEventBus();
    }
}
